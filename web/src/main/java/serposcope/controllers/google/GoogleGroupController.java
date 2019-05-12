/*
 * Serposcope - SEO rank checker https://serposcope.serphacker.com/
 *
 * Copyright (c) 2016 SERP Hacker
 * @author Pierre Nogues <support@serphacker.com>
 * @license https://opensource.org/licenses/MIT MIT License
 */
package serposcope.controllers.google;

import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.serphacker.serposcope.db.base.BaseDB;
import com.serphacker.serposcope.db.base.RunDB;
import com.serphacker.serposcope.db.google.GoogleDB;
import com.serphacker.serposcope.inteligenciaseo.*;
import com.serphacker.serposcope.models.base.Event;
import com.serphacker.serposcope.models.base.Group;
import com.serphacker.serposcope.models.base.Run;
import com.serphacker.serposcope.models.base.User;
import com.serphacker.serposcope.models.google.GoogleSearch;
import com.serphacker.serposcope.models.google.GoogleTarget;
import com.serphacker.serposcope.models.google.GoogleTarget.PatternType;
import com.serphacker.serposcope.models.google.GoogleTargetSummary;
import com.serphacker.serposcope.scraper.google.GoogleCountryCode;
import com.serphacker.serposcope.scraper.google.GoogleDevice;
import com.serphacker.serposcope.task.TaskManager;
import ninja.*;
import ninja.i18n.Messages;
import ninja.params.Param;
import ninja.params.Params;
import ninja.session.FlashScope;
import ninja.utils.ResponseStreams;
import org.apache.commons.lang3.StringEscapeUtils;
import org.jsoup.Jsoup;
import org.jsoup.safety.Whitelist;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import serposcope.controllers.GroupController;
import serposcope.filters.AdminFilter;
import serposcope.filters.XSRFFilter;
import serposcope.helpers.Validator;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.IDN;
import java.time.LocalDate;
import java.util.*;
import java.util.zip.GZIPOutputStream;

import static com.serphacker.serposcope.scraper.google.GoogleDevice.SMARTPHONE;

@SuppressWarnings("Guava")
@Singleton
public class GoogleGroupController extends GoogleController {

    private static final Logger LOG = LoggerFactory.getLogger(GoogleGroupController.class);
    private Messages messages;

    @Inject
    GoogleGroupController(Messages messages) {
        this.messages = messages;
    }

    @Inject
    Router router;

    @Inject
    BaseDB baseDB;

    @Inject
    GoogleDB googleDB;

    @Inject
    TaskManager taskManager;

    @Inject
    ReportsDB reportsDB;

    @Inject
    SearchSettingsDB settingsDB;

    @Inject
    InactiveKeywordsDB inactiveKeywordsDB;

    @Inject
    Messages msg;

    @Inject
    final Object searchLock = new Object();

    private interface GroupSearchVisitor {
        void visit(Group group, GoogleSearch search);
        Result onFinished(Group group);
        Result onError(Group group);
    }

    public Result view(Context context) {

        Group group = context.getAttribute("group", Group.class);

        Map<Integer, GoogleTargetSummary> summaryByTagetId = new HashMap<>();
        Map<Integer, List<Integer>> scoreHistoryByTagetId = new HashMap<>();

        Run lastRun = baseDB.run.findLast(group.getModule(), RunDB.STATUSES_DONE, null);
        if (lastRun != null) {
            List<GoogleTargetSummary> summaries = googleDB.targetSummary.list(lastRun.getId());
            for (GoogleTargetSummary summary : summaries) {
                if (summary != null) {
                    summaryByTagetId.put(summary.getTargetId(), summary);
                    List<Integer> scoreHistory = googleDB.targetSummary.listScoreHistory(group.getId(), summary.getTargetId(), 30);
                    int missingScore = 30 - scoreHistory.size();
                    for (int i = 0; i < missingScore; i++) {
                        scoreHistory.add(0, 0);
                    }
                    scoreHistoryByTagetId.put(summary.getTargetId(), scoreHistory);
                }
            }
        }

        User user = context.getAttribute("user", User.class);
        return Results
                .ok()
                .render("events", user.isAdmin() ? baseDB.event.list(group, null, null) : "[]")
                .render("default", googleDB.options.get())
                .render("searchesSize", context.getAttribute("searches", List.class).size())
                .render("reportsCount", 0)
                .render("reports", context.getAttribute("reports"))
                .render("targets", context.getAttribute("targets"))
                .render("summaries", summaryByTagetId)
                .render("histories", scoreHistoryByTagetId)
                .render("categories", settingsDB.listCategories(user))
                .render("tags", settingsDB.listTags(user));
    }

    public Result jsonSearches(Context context) {
        List<GoogleSearch> searches = context.getAttribute("searches", List.class);
        if (searches.isEmpty()) {
            return Results.json().renderRaw("[]");
        }

        return Results.ok()
                .json()
                .render((Context context0, Result result) -> {
                    PrintWriter writer = null;
                    OutputStream os = null;
                    try {

                        String acceptEncoding = context0.getHeader("Accept-Encoding");
                        if (acceptEncoding != null && acceptEncoding.contains("gzip")) {
                            result.addHeader("Content-Encoding", "gzip");
                        }

                        ResponseStreams response = context0.finalizeHeaders(result);
                        os = response.getOutputStream();
                        if (acceptEncoding != null && acceptEncoding.contains("gzip")) {
                            os = new GZIPOutputStream(os);
                        }

                        writer = new PrintWriter(os);
                        writer.append("[");
                        for (int i = 0; i < searches.size(); i++) {
                            GoogleSearch search = searches.get(i);
                            String category = settingsDB.getCategory(search.getId());
                            boolean isAdminOnly = settingsDB.getIsOnlyAdmins(search.getId());
                            Integer volume = settingsDB.getVolume(search.getId());
                            Double cpc = settingsDB.getCPC(search.getId());
                            Integer competition = settingsDB.getCompetition(search.getId());
                            String tag = settingsDB.getTag(search.getId());
                            writer
                                    .append("{")
                                    .append("\"id\":")
                                    .append(Integer.toString(search.getId()))
                                    .append(",")
                                    .append("\"keyword\":\"")
                                    .append(StringEscapeUtils.escapeJson(search.getKeyword()))
                                    .append("\",")
                                    .append("\"country\":\"")
                                    .append(search.getCountry().name())
                                    .append("\",")
                                    .append("\"device\":\"")
                                    .append(SMARTPHONE.equals(search.getDevice()) ? 'M' : 'D')
                                    .append("\",")
                                    .append("\"local\":\"")
                                    .append(search.getLocal() == null ? "" : StringEscapeUtils.escapeJson(search.getLocal()))
                                    .append("\",")
                                    .append("\"datacenter\":\"")
                                    .append(search.getDatacenter() == null ? "" : StringEscapeUtils.escapeJson(search.getDatacenter()))
                                    .append("\",")
                                    .append("\"competition\":\"")
                                    .append(competition == null ? "0" : competition.toString())
                                    .append("\",")
                                    .append("\"volume\":")
                                    .append(volume == null ? "0" : volume.toString())
                                    .append(",")
                                    .append("\"isAdminOnly\":")
                                    .append(isAdminOnly ? "true" : "false")
                                    .append(",")
                                    .append("\"cpc\":")
                                    .append(cpc == null ? "0.0" : cpc.toString())
                                    .append(",")
                                    .append("\"tag\":")
                                    .append(tag == null ? "null" : String.format("\"%s\"", StringEscapeUtils.escapeJson(tag)))
                                    .append(",")
                                    .append("\"category\":")
                                    .append(category == null ? "null" : String.format("\"%s\"", StringEscapeUtils.escapeJson(category)))
                                    .append("}")
                            ;
                            if (i != searches.size() - 1) {
                                writer.append(",");
                            }
                        }
                        writer.append("]");
                    } catch (Exception ex) {
                        LOG.warn("HTTP error", ex);
                    } finally {
                        if (os != null) {
                            try {
                                writer.close();
                                os.close();
                            } catch (Exception ex) {
                            }
                        }
                    }
                });

    }

    private boolean validateAddParameters(String[] keywords, Object[]... arrays) {
        if (keywords == null)
            return false;
        for (Object[] array : arrays) {
            if (array == null || array.length != keywords.length) {
                return false;
            }
        }
        return true;
    }

    private Double getAsDouble(String input) {
        int decimalSeparatorPosition = input.indexOf('.');
        if (decimalSeparatorPosition == -1) {
            decimalSeparatorPosition = input.indexOf(',');
        }
        if (decimalSeparatorPosition == -1)
            return Double.parseDouble(input);
        input = input.substring(0, decimalSeparatorPosition) + input.substring(decimalSeparatorPosition + 1);
        return Double.parseDouble(input) / Math.pow(10.0, input.length() - decimalSeparatorPosition);
    }

    @FilterWith({
            XSRFFilter.class,
            AdminFilter.class
    })
    public Result addSearch(
            Context context,
            @Params("keyword[]") String[] keywords,
            @Params("country[]") String[] country,
            @Params("datacenter[]") String[] datacenters,
            @Params("device[]") Integer[] devices,
            @Params("local[]") String[] locals,
            @Params("custom[]") String[] customs,
            @Params("competition[]") Integer[] competitions,
            @Params("category[]") String[] categories,
            @Params("tag[]") String[] tags,
            @Params("volume[]") Integer[] volumes,
            @Params("cpc[]") String[] cpcs,
            @Params("invisible[]") Boolean[] invisible
    ) {
        FlashScope flash = context.getFlashScope();
        Group group = context.getAttribute("group", Group.class);

        Map<String, Object> map = new HashMap<>();
        map.put("keyword", keywords);
        map.put("country", country);
        map.put("datacenter", datacenters);
        map.put("device", keywords);
        map.put("local", keywords);
        map.put("custom", keywords);
        map.put("competition", keywords);
        map.put("category", keywords);
        map.put("tag", keywords);
        map.put("volume", keywords);
        map.put("cpc", keywords);
        map.put("invisible", keywords);
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            if (entry.getValue() == null) {
                flash.error("error.invalidParameters");
                LOG.warn(String.format("`%s' is `null'", entry.getKey()));
                return Results.redirect(router.getReverseRoute(GoogleGroupController.class, "view", "groupId", group.getId()));
            }
        }

        Set<SearchSettings> settings = new HashSet<>();
        Set<GoogleSearch> searches = new HashSet<>();
        for (int i = 0; i < keywords.length; i++) {
            GoogleSearch search = new GoogleSearch();
            if (keywords[i].isEmpty()) {
                flash.error("admin.google.keywordEmpty");
                return Results.redirect(router.getReverseRoute(GoogleGroupController.class, "view", "groupId", group.getId()));
            }
            search.setKeyword(keywords[i]);
            GoogleCountryCode countryCode = null;
            if (country[i] != null) {
                try {
                    countryCode = GoogleCountryCode.valueOf(country[i].toUpperCase());
                } catch (Exception ex) {
                    LOG.warn("Country Code", ex);
                }
            }
            if (countryCode == null) {
                flash.error("admin.google.invalidCountry");
                return Results.redirect(router.getReverseRoute(GoogleGroupController.class, "view", "groupId", group.getId()));
            }
            search.setCountry(countryCode);
            if (!datacenters[i].isEmpty()) {
                if (!Validator.isIPv4(datacenters[i])) {
                    flash.error("error.invalidIP");
                    return Results.redirect(router.getReverseRoute(GoogleGroupController.class, "view", "groupId", group.getId()));
                }
                search.setDatacenter(datacenters[i]);
            }
            if (devices[i] != null && devices[i] >= 0 && devices[i] < GoogleDevice.values().length) {
                search.setDevice(GoogleDevice.values()[devices[i]]);
            } else {
                search.setDevice(GoogleDevice.DESKTOP);
            }
            if (!Validator.isEmpty(locals[i])) {
                search.setLocal(locals[i]);
            }
            if (!Validator.isEmpty(customs[i])) {
                search.setCustomParameters(customs[i]);
            }
            Double cpc = getAsDouble(cpcs[i]);
            settings.add(new SearchSettings(
                    search,
                    categories[i],
                    competitions[i],
                    volumes[i],
                    invisible[i],
                    cpc,
                    tags[i]
            ));
            searches.add(search);
        }

        List<GoogleSearch> knownSearches = new ArrayList<>();
        synchronized (searchLock) {
            for (GoogleSearch search : searches) {
                int id = googleDB.search.getId(search);
                if (id > 0) {
                    search.setId(id);
                    knownSearches.add(search);
                }
            }
            googleDB.search.insert(searches, group.getId());
            settingsDB.insert(settings);
        }
        googleDB.serpRescan.rescan(null, getTargets(context), knownSearches, false);
        flash.success("google.group.searchInserted");

        return Results.redirect(router.getReverseRoute(GoogleGroupController.class, "view", "groupId", group.getId()) + "#tab-searches");
    }

    @FilterWith({
            AdminFilter.class
    })
    public Result addReport(
            Context context,
            @Params("name[]") String[] names,
            @Params("iframe[]") String[] iframes
    ) {
        FlashScope flash = context.getFlashScope();
        Group group = context.getAttribute("group", Group.class);
        if (names == null || names.length == 0
                || iframes == null || iframes.length == 0
                || names.length != iframes.length) {
            flash.error("error.invalidParameters");
            return Results.redirect(router.getReverseRoute(GoogleGroupController.class, "view", "groupId", group.getId()));
        }
        Set<Report> reports = new HashSet<>();
        for (int i = 0; i < names.length; i++) {
            String name = names[i];
            String iframe = iframes[i];

            if (name != null) {
                name = name.replaceAll("(^\\s+)|(\\s+$)", "");
            }

            if (iframe != null) {
                iframe = iframe.replaceAll("(^\\s+)|(\\s+$)", "");
            }

            if (Validator.isEmpty(name)) {
                flash.error("error.invalidName");
                return Results.redirect(router.getReverseRoute(GoogleGroupController.class, "view", "groupId", group.getId()) + "#tab-reports");
            }
            reports.add(new Report(group.getId(), name, iframe));
        }

        if (reportsDB.insertReports(reports) != reports.size()) {
            flash.error("error.internalError");
            return Results.redirect(router.getReverseRoute(GoogleGroupController.class, "view", "groupId", group.getId()) + "#tab-reports");
        }
        flash.success("inteligenciaseo.reportCreated");
        return Results.redirect(router.getReverseRoute(GoogleGroupController.class, "view", "groupId", group.getId()) + "#tab-reports");
    }

    @FilterWith({
            XSRFFilter.class,
            AdminFilter.class
    })
    public Result addTarget(
            Context context,
            @Param("target-radio") String targetType,
            @Params("name[]") String[] names,
            @Params("pattern[]") String[] patterns
    ) {
        FlashScope flash = context.getFlashScope();
        Group group = context.getAttribute("group", Group.class);

        if (targetType == null
                || names == null || names.length == 0
                || patterns == null || patterns.length == 0
                || names.length != patterns.length) {
            flash.error("error.invalidParameters");
            return Results.redirect(router.getReverseRoute(GoogleGroupController.class, "view", "groupId", group.getId()));
        }

        Set<GoogleTarget> targets = new HashSet<>();
        for (int i = 0; i < names.length; i++) {
            String name = names[i];
            String pattern = patterns[i];

            if (name != null) {
                name = name.replaceAll("(^\\s+)|(\\s+$)", "");
            }

            if (pattern != null) {
                pattern = pattern.replaceAll("(^\\s+)|(\\s+$)", "");
            }

            if (Validator.isEmpty(name)) {
                flash.error("error.invalidName");
                return Results.redirect(router.getReverseRoute(GoogleGroupController.class, "view", "groupId", group.getId()));
            }

            PatternType type = null;
            try {
                type = PatternType.valueOf(targetType);
            } catch (Exception ex) {
                flash.error("error.invalidTargetType");
                return Results.redirect(router.getReverseRoute(GoogleGroupController.class, "view", "groupId", group.getId()));
            }

            if (PatternType.DOMAIN.equals(type) || PatternType.SUBDOMAIN.equals(type)) {
                try {
                    pattern = IDN.toASCII(pattern);
                } catch (Exception ex) {
                    pattern = null;
                }
            }

            if (!GoogleTarget.isValidPattern(type, pattern)) {
                flash.error("error.invalidPattern");
                return Results.redirect(router.getReverseRoute(GoogleGroupController.class, "view", "groupId", group.getId()));
            }

            targets.add(new GoogleTarget(group.getId(), name, type, pattern));
        }

        if (googleDB.target.insert(targets) < 1) {
            flash.error("error.internalError");
            return Results.redirect(router.getReverseRoute(GoogleGroupController.class, "view", "groupId", group.getId()));
        }
        googleDB.serpRescan.rescan(null, targets, getSearches(context), true);

        Run runningGoogleTask = taskManager.getRunningGoogleTask();
        if (runningGoogleTask != null) {
            flash.put("warning", msg.get("google.group.websiteInsertedWhileRun", context, Optional.absent(), runningGoogleTask.getId()).or(""));
        } else {
            flash.success("google.group.websiteInserted");
        }

        return Results.redirect(router.getReverseRoute(GoogleGroupController.class, "view", "groupId", group.getId()));
    }

    @FilterWith({
            XSRFFilter.class,
            AdminFilter.class
    })
    public Result renameTarget(
            Context context,
            @Param("name") String name,
            @Param("id") Integer targetId) {

        FlashScope flash = context.getFlashScope();
        Group group = context.getAttribute("group", Group.class);

        GoogleTarget target = getTarget(context, targetId);
        if (target == null) {
            flash.error("error.invalidWebsite");
            return Results.redirect(router.getReverseRoute(GoogleGroupController.class, "view", "groupId", group.getId()));
        }

        if (name != null) {
            name = name.replaceAll("(^\\s+)|(\\s+$)", "");
        }

        if (Validator.isEmpty(name)) {
            flash.error("error.invalidName");
            return Results.redirect(router.getReverseRoute(GoogleGroupController.class, "view", "groupId", group.getId()));
        }

        target.setName(name);
        googleDB.target.rename(target);

        flash.success("google.group.websiteRenamed");
        return Results.redirect(router.getReverseRoute(GoogleGroupController.class, "view", "groupId", group.getId()));
    }

    @FilterWith({
            XSRFFilter.class,
            AdminFilter.class
    })
    public Result refreshVolumes(
            Context context,
            @Params("id[]") String[] ids
    ) {
        Group group = context.getAttribute("group", Group.class);
        if (ids == null) {
            return Results.redirect(router.getReverseRoute(GoogleGroupController.class, "view", "groupId", group.getId()) + "#tab-searches");
        }
        // List<Keyword> keywords = new ArrayList<Keyword>();
        System.out.println("refreshing keyword search volumes");
        for (String id : ids) {
            GoogleSearch search = googleDB.search.find(Integer.valueOf(id));
            System.out.printf("\t%s\n", search.getKeyword());
        }
        return Results.redirect(router.getReverseRoute(GoogleGroupController.class, "view", "groupId", group.getId()) + "#tab-searches");
    }

    private Result forEachSearch(Context context, String[] ids, GroupSearchVisitor visitor) {
        FlashScope flash = context.getFlashScope();
        Group group = context.getAttribute("group", Group.class);
        if (ids == null || ids.length == 0) {
            flash.error("error.noSearchSelected");
            return visitor.onError(group);
        }
        List<GoogleSearch> searches = new ArrayList<>();
        for (String id : ids) {
            GoogleSearch search = null;
            try {
                search = getSearch(context, Integer.parseInt(id));
            } catch (Exception ignored) {
            }
            if (search == null) {
                flash.error("error.invalidSearch");
                return visitor.onError(group);
            }
            searches.add(search);
        }
        // TODO FIX ME locking until database modification done
        if (taskManager.isGoogleRunning()) {
            flash.error("admin.google.errorTaskRunning");
        }
        for (GoogleSearch search : searches) {
            visitor.visit(group, search);
        }
        return visitor.onFinished(group);
    }


    @FilterWith({
            XSRFFilter.class,
            AdminFilter.class
    })
    public Result deleteInactiveKeywords(Context context) {
        FlashScope flash = context.getFlashScope();
        Group group = context.getAttribute("group", Group.class);
        List<Integer> list = inactiveKeywordsDB.getInactiveSearchIdsForGroup(group);
        Optional<String> language = Optional.of("es");
        InactiveKeywordsDB.Deleter<Result> deleter = new InactiveKeywordsDB.Deleter<Result>() {
            @Override
            public Result onAlreadyClean() {
                flash.success("message.yourHouseIsClean");
                return Results.redirect(router.getReverseRoute(GoogleGroupController.class, "view", "groupId", group.getId()) + "#tab-searches");
            }

            @Override
            public Result onError(int count) {
                Object[] parameters = {Integer.toString(count)};
                Optional<String> message = messages.get("message.noKeywordsDeleted", language, parameters);
                // Display success message
                if (message.isPresent()) {
                    flash.success(message.get());
                }
                return Results.redirect(router.getReverseRoute(GoogleGroupController.class, "view", "groupId", group.getId()) + "#tab-searches");
            }

            @Override
            public Result onPartialSuccess(int actualCount, int expectedCount) {
                Object[] parameters = {Integer.toString(actualCount), Integer.toString(expectedCount)};
                Optional<String> message = messages.get("message.someKeywordsDeleted", language, parameters);
                // Display success message
                if (message.isPresent()) {
                    flash.success(message.get());
                }
                return Results.redirect(router.getReverseRoute(GoogleGroupController.class, "view", "groupId", group.getId()) + "#tab-searches");
            }

            @Override
            public Result onSuccess(int count) {
                Object[] parameters = {Integer.toString(count)};
                Optional<String> message = messages.get("message.allKeywordsDeleted", language, parameters);
                // Display success message
                if (message.isPresent()) {
                    flash.success(message.get());
                }
                return Results.redirect(router.getReverseRoute(GoogleGroupController.class, "view", "groupId", group.getId()) + "#tab-searches");
            }
        };
        return inactiveKeywordsDB.removeFromList(list, deleter);
    }

    @FilterWith({
            XSRFFilter.class,
            AdminFilter.class
    })
    public Result delSearch(
            Context context,
            @Params("id[]") String[] ids
    ) {
        GroupSearchVisitor visitor = new GroupSearchVisitor() {
            @Override
            public void visit(Group group, GoogleSearch search) {
                deleteSearch(group, search);
            }

            @Override
            public Result onFinished(Group group) {
                return Results.redirect(router.getReverseRoute(GoogleGroupController.class, "view", "groupId", group.getId()) + "#tab-searches");
            }

            @Override
            public Result onError(Group group) {
                return Results.redirect(router.getReverseRoute(GoogleGroupController.class, "view", "groupId", group.getId()));
            }
        };
        return forEachSearch(context, ids, visitor);
    }

    public Result deleteReport(
            Context context,
            @Params("id[]") Integer[] ids
    ) {
        FlashScope flash = context.getFlashScope();
        Group group = context.getAttribute("group", Group.class);
        if (ids == null || ids.length == 0) {
            flash.error("error.noSearchSelected");
            return Results.redirect(router.getReverseRoute(GoogleGroupController.class, "view", "groupId", group.getId()) + "#tab-reports");
        }
        if (reportsDB.delete(ids) == 0) {
            flash.error("error.internalError");
            return Results.redirect(router.getReverseRoute(GoogleGroupController.class, "view", "groupId", group.getId()) + "#tab-reports");
        }
        return Results.redirect(router.getReverseRoute(GoogleGroupController.class, "view", "groupId", group.getId()) + "#tab-reports");
    }

    @FilterWith({
            XSRFFilter.class,
            AdminFilter.class
    })
    public Result delTarget(
            Context context,
            @Params("id[]") String[] ids
    ) {
        FlashScope flash = context.getFlashScope();
        Group group = context.getAttribute("group", Group.class);

        // TODO FIX ME locking until database modification done
        if (taskManager.isGoogleRunning()) {
            flash.error("admin.google.errorTaskRunning");
            return Results.redirect(router.getReverseRoute(GoogleGroupController.class, "view", "groupId", group.getId()));
        }

        if (ids == null || ids.length == 0) {
            flash.error("error.noWebsiteSelected");
            return Results.redirect(router.getReverseRoute(GoogleGroupController.class, "view", "groupId", group.getId()));
        }

        for (String id : ids) {
            GoogleTarget target = null;
            try {
                target = getTarget(context, Integer.parseInt(id));
            } catch (Exception ex) {
                target = null;
            }

            if (target == null) {
                flash.error("error.invalidWebsite");
                return Results.redirect(router.getReverseRoute(GoogleGroupController.class, "view", "groupId", group.getId()));
            }

            googleDB.targetSummary.deleteByTarget(target.getId());
            googleDB.rank.deleteByTarget(group.getId(), target.getId());
            googleDB.target.delete(target.getId());
        }

        return Results.redirect(router.getReverseRoute(GoogleGroupController.class, "view", "groupId", group.getId()));
    }

    @FilterWith({
            XSRFFilter.class,
            AdminFilter.class
    })
    public Result delete(Context context) {
        FlashScope flash = context.getFlashScope();
        Group group = context.getAttribute("group", Group.class);

        // TODO FIX ME locking until database modification done
        if (taskManager.isGoogleRunning()) {
            flash.error("admin.google.errorTaskRunning");
            return Results.redirect(router.getReverseRoute(GoogleGroupController.class, "view", "groupId", group.getId()));
        }

        List<GoogleTarget> targets = googleDB.target.list(Arrays.asList(group.getId()));
        for (GoogleTarget target : targets) {
            googleDB.targetSummary.deleteByTarget(target.getId());
            googleDB.rank.deleteByTarget(group.getId(), target.getId());
            googleDB.target.delete(target.getId());
        }

        List<GoogleSearch> searches = googleDB.search.listByGroup(Arrays.asList(group.getId()));
        for (GoogleSearch search : searches) {
            deleteSearch(group, search);
        }

        baseDB.event.delete(group);
        baseDB.user.delPerm(group);
        if (!baseDB.group.delete(group)) {
            flash.error("admin.google.failedDeleteGroup");
            return Results.redirect(router.getReverseRoute(GoogleGroupController.class, "view", "groupId", group.getId()));
        } else {
            flash.success("admin.google.groupDeleted");
            return Results.redirect(router.getReverseRoute(GroupController.class, "groups"));
        }

    }

    @FilterWith({
            XSRFFilter.class,
            AdminFilter.class
    })
    public Result exportSearches(
            Context context,
            @Params("id[]") String[] ids
    ) {
        GroupSearchVisitor visitor = new GroupSearchVisitor() {
            private StringBuilder builder = new StringBuilder();

            @Override
            public void visit(Group group, GoogleSearch search) {
                builder.append(StringEscapeUtils.escapeCsv(search.getKeyword())).append(",");
                builder.append(search.getCountry()).append(",");
                builder.append(search.getDatacenter() != null ? search.getDatacenter() : "").append(",");
                builder.append(search.getDevice() != null ? (search.getDevice() == GoogleDevice.DESKTOP ? "desktop" : "mobile") : "").append(",");
                builder.append(StringEscapeUtils.escapeCsv(search.getLocal() != null ? search.getLocal() : "")).append(",");
                builder.append(StringEscapeUtils.escapeCsv(search.getCustomParameters() != null ? search.getCustomParameters() : "")).append("\n");
            }

            @Override
            public Result onFinished(Group group) {
                return Results.ok()
                        .text()
                        .render(builder.toString())
                        ;
            }

            @Override
            public Result onError(Group group) {
                return null;
            }
        };
        return forEachSearch(context, ids, visitor);
    }

    protected void deleteSearch(Group group, GoogleSearch search) {
        synchronized (searchLock) {
            googleDB.search.deleteFromGroup(search, group.getId());
            googleDB.rank.deleteBySearch(group.getId(), search.getId());
            if (!googleDB.search.hasGroup(search)) {
                googleDB.serp.deleteBySearch(search.getId());
                googleDB.search.delete(search);
            }
        }
    }

    @FilterWith({
            XSRFFilter.class,
            AdminFilter.class
    })
    public Result addEvent(Context context,
                           @Param("day") String day,
                           @Param("title") String title,
                           @Param("description") String description,
                           @Param("redir-search") Integer redirSearchId,
                           @Param("redir-target") Integer redirTargetId
    ) {
        FlashScope flash = context.getFlashScope();
        Group group = context.getAttribute("group", Group.class);

        Event event = new Event();
        event.setGroupId(group.getId());
        try {
            event.setDay(LocalDate.parse(day));
        } catch (Exception ex) {
        }

        if (event.getDay() == null) {
            flash.error("error.invalidDate");
            return Results.redirect(router.getReverseRoute(GoogleGroupController.class, "view", "groupId", group.getId()));
        }

        if (Validator.isEmpty(title)) {
            flash.error("error.invalidTitle");
            return Results.redirect(router.getReverseRoute(GoogleGroupController.class, "view", "groupId", group.getId()));
        }

        if (baseDB.event.find(group, event.getDay()) != null) {
            flash.error("google.group.alreadyEventForThisDate");
            return Results.redirect(router.getReverseRoute(GoogleGroupController.class, "view", "groupId", group.getId()));
        }

        event.setTitle(title);
        event.setDescription(Jsoup.clean(description == null ? "" : description, Whitelist.basic()));

        if (!baseDB.event.insert(event)) {
            flash.error("error.internalError");
            return Results.redirect(router.getReverseRoute(GoogleGroupController.class, "view", "groupId", group.getId()));
        }

        flash.success("google.group.eventInserted");
        if (redirSearchId != null) {
            return Results.redirect(router.getReverseRoute(GoogleSearchController.class, "search", "groupId", group.getId(),
                    "searchId", redirSearchId));
        }
        if (redirTargetId != null) {
            return Results.redirect(router.getReverseRoute(GoogleTargetController.class, "target", "groupId", group.getId(),
                    "targetId", redirTargetId));
        }

        return Results.redirect(router.getReverseRoute(GoogleGroupController.class, "view", "groupId", group.getId()));
    }

    @FilterWith({
            XSRFFilter.class,
            AdminFilter.class
    })
    public Result delEvent(Context context, @Param("day") String day) {
        FlashScope flash = context.getFlashScope();
        Group group = context.getAttribute("group", Group.class);

        Event event = new Event();
        event.setGroupId(group.getId());
        try {
            event.setDay(LocalDate.parse(day));
        } catch (Exception ex) {
        }
        if (event.getDay() == null) {
            flash.error("error.invalidDate");
            return Results.redirect(router.getReverseRoute(GoogleGroupController.class, "view", "groupId", group.getId()));
        }

        baseDB.event.delete(event);
        flash.success("google.group.eventDeleted");
        return Results.redirect(router.getReverseRoute(GoogleGroupController.class, "view", "groupId", group.getId()));
    }

    @FilterWith({
            XSRFFilter.class,
            AdminFilter.class
    })
    public Result rename(Context context, @Param("name") String name) {
        FlashScope flash = context.getFlashScope();
        Group group = context.getAttribute("group", Group.class);

        if (Validator.isEmpty(name)) {
            flash.error("error.invalidName");
            return Results.redirect(router.getReverseRoute(GoogleGroupController.class, "view", "groupId", group.getId()));
        }

        group.setName(name);
        baseDB.group.update(group);

        flash.success("google.group.groupRenamed");
        return Results.redirect(router.getReverseRoute(GoogleGroupController.class, "view", "groupId", group.getId()));
    }

    public Result jsonReportSuggest(
            Context context,
            @Param("query") String query
    ) {

        StringBuilder builder = new StringBuilder("[");
        getReports(context).stream()
                .limit(10)
                .forEach((g) -> {
                    builder.append("{")
                            .append("\"id\":").append(g.getId()).append(",")
                            .append("\"name\":\"").append(StringEscapeUtils.escapeJson(g.getName())).append("\",")
                            .append("\"group\":").append(g.getGroupId())
                            .append("},");
                });
        if (builder.length() > 1) {
            builder.deleteCharAt(builder.length() - 1);
        }
        builder.append("]");

        return Results.json().renderRaw(builder.toString());
    }

    public Result jsonTargetSuggest(
            Context context,
            @Param("query") String query
    ) {

        StringBuilder builder = new StringBuilder("[");
        getTargets(context).stream()
                .filter((g) -> query == null ? true : g.getName().contains(query))
                .sorted((o1, o2) -> o1.getId() - o2.getId())
                .limit(10)
                .forEach((g) -> {
                    builder.append("{")
                            .append("\"id\":").append(g.getId()).append(",")
                            .append("\"name\":\"").append(StringEscapeUtils.escapeJson(g.getName())).append("\",")
                            .append("\"group\":").append(g.getGroupId())
                            .append("},");
                });
        if (builder.length() > 1) {
            builder.deleteCharAt(builder.length() - 1);
        }
        builder.append("]");

        return Results.json().renderRaw(builder.toString());
    }

    public Result jsonSearchSuggest(
            Context context,
            @Param("query") String query
    ) {
        StringBuilder builder = new StringBuilder("[");
        Group group = context.getAttribute("group", Group.class);
        User user = context.getAttribute("user", User.class);
        getSearches(context).stream()
                .filter((g) -> query == null ? true : g.getKeyword().contains(query))
                .filter((g) -> user.isAdmin() || !settingsDB.getIsOnlyAdmins(g.getId()))
                .sorted((o1, o2) -> o1.getId() - o2.getId())
                .limit(10)
                .forEach((g) -> {
                    builder.append("{")
                            .append("\"id\":").append(g.getId()).append(",")
                            .append("\"group\":").append(group.getId()).append(",")
                            .append("\"name\":\"").append(StringEscapeUtils.escapeJson(g.getKeyword())).append("\"")
                            .append("},");
                });
        if (builder.length() > 1) {
            builder.deleteCharAt(builder.length() - 1);
        }
        builder.append("]");

        return Results.json().renderRaw(builder.toString());
    }

}
