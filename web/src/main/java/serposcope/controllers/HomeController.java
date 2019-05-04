/* 
 * Serposcope - SEO rank checker https://serposcope.serphacker.com/
 * 
 * Copyright (c) 2016 SERP Hacker
 * @author Pierre Nogues <support@serphacker.com>
 * @license https://opensource.org/licenses/MIT MIT License
 */
package serposcope.controllers;

import com.serphacker.serposcope.db.base.BaseDB;
import com.serphacker.serposcope.db.base.RunDB;
import com.serphacker.serposcope.db.google.GoogleDB;
import com.serphacker.serposcope.inteligenciaseo.SearchSettingsDB;
import com.serphacker.serposcope.models.base.Config;
import com.serphacker.serposcope.models.base.Group;
import com.serphacker.serposcope.models.base.Group.Module;
import com.serphacker.serposcope.models.base.Run;
import com.serphacker.serposcope.models.base.User;
import com.serphacker.serposcope.models.google.GoogleTarget;
import com.serphacker.serposcope.models.google.GoogleTargetSummary;
import ninja.*;
import ninja.params.PathParam;
import serposcope.filters.AuthFilter;
import serposcope.lifecycle.DBSizeUtils;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.time.LocalDate;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Singleton
@FilterWith(AuthFilter.class)
public class HomeController extends BaseController {
    
    @Inject
    Router router;
    
    @Inject
    BaseDB baseDB;
    
    @Inject
    GoogleDB googleDB;
    
    @Inject
    DBSizeUtils dbSizeUtils;

    @Inject
    SearchSettingsDB settingsDB;
    
    public static class TargetHomeEntry {

        public TargetHomeEntry(String groupName, GoogleTarget target, GoogleTargetSummary summary, List<Integer> scoreHistory) {
            this.groupName = groupName;
            this.target = target;
            this.summary = summary;
            this.scoreHistory = scoreHistory;
        }
        public final String groupName;
        public final GoogleTarget target;
        public final GoogleTargetSummary summary;
        public final List<Integer> scoreHistory;
    }

    
    public Result home(Context context) {
        
        String diskUsage = dbSizeUtils.getDbUsageFormatted();
        String diskFree = dbSizeUtils.getDiskFreeFormatted();
        
        String display = context.getParameter("display" , baseDB.config.getConfig().getDisplayHome());
        if(!Config.VALID_DISPLAY_HOME.contains(display)){
            display = Config.DEFAULT_DISPLAY_HOME;
        }
        
        List<Group> groups = (List<Group>) context.getAttribute("groups");
        Run currentRun = baseDB.run.findLast(Module.GOOGLE, RunDB.STATUSES_RUNNING, null);
        Run lastRun = baseDB.run.findLast(Module.GOOGLE, RunDB.STATUSES_DONE, null);
        if(lastRun == null){
            return Results
                .ok()
                .template("/serposcope/views/HomeController/" + display + ".ftl.html")
                .render("display", display)
                .render("summaries", Collections.EMPTY_LIST)
                .render("currentRun", currentRun)
                .render("lastRun", lastRun)
                .render("groups", groups)
                .render("lastlog", LocalDate.now().toString() + ".log")
                .render("diskUsage", diskUsage)
                .render("diskFree", diskFree)
                ;
        }
        
        List<TargetHomeEntry> summaries = new ArrayList<>();

        User user = context.getAttribute("user", User.class);
        Map<Integer, GoogleTargetSummary> summariesByTarget = googleDB.targetSummary.listForUser(
            lastRun.getId(), user).stream().collect(
                Collectors.toMap(GoogleTargetSummary::getTargetId, Function.identity())
            );

        List<GoogleTarget> targets = googleDB.target.list(groups.stream().map(Group::getId).collect(Collectors.toList()));

        Map<Integer, Group> groupById = groups.stream().collect(Collectors.toMap(Group::getId, Function.identity()));


        for (GoogleTarget target : targets) {
            GoogleTargetSummary summary = summariesByTarget.get(target.getId());
            if(summary != null){
                List<Integer> scoreHistory;

                if (user.isAdmin()) {
                    scoreHistory = googleDB.targetSummary.listScoreHistory(target.getGroupId(), target.getId(), 30);
                    int missingScore = 30 - scoreHistory.size();
                    for (int i = 0; i < missingScore; i++) {
                        scoreHistory.add(0, 0);
                    }
                } else {
                    scoreHistory = new ArrayList<>();
                }
                summaries.add(new TargetHomeEntry(groupById.get(target.getGroupId()).getName(), target, summary, scoreHistory));
            }
        }
        
        Set<Integer> searchIds = new HashSet<>();

        for (TargetHomeEntry homeEntry : summaries) {
            GoogleTargetSummary summary = homeEntry.summary;
            summary.visitReferencedSearchId(user, settingsDB, searchIds);
        }

        return Results
            .ok()
            .template("/serposcope/views/HomeController/" + display + ".ftl.html")
            .render("display", display)
            .render("groups", context.getAttribute("groups"))
            .render("currentRun", currentRun)
            .render("lastRun", lastRun)
            .render("lastRuns", baseDB.run.listByStatus(null, 7l, 0l))
            .render("hasTarget", googleDB.target.hasTarget())
            .render("summaries", summaries)
            .render("searches", googleDB.search.mapBySearchId(searchIds))
            .render("lastlog", LocalDate.now().toString() + ".log")
            .render("diskUsage", diskUsage)
            .render("diskFree", diskFree)            
            ;
    }

    public Result taskStatus(@PathParam("taskId") Integer taskId){
        
        Map<String,Object> map = new HashMap<>();
        
        if(taskId != null){
            Run run = baseDB.run.find(taskId);
            if(run != null){
                map.put("progress", run.getProgress());
                map.put("status", run.getStatus());
            }
        }
        
        return Results.ok().json().render(map);
    }
    
}
