/* 
 * Serposcope - SEO rank checker https://serposcope.serphacker.com/
 * 
 * Copyright (c) 2016 SERP Hacker
 * @author Pierre Nogues <support@serphacker.com>
 * @license https://opensource.org/licenses/MIT MIT License
 */
package serposcope.controllers.google;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Optional;
import com.serphacker.serposcope.db.base.BaseDB;
import com.serphacker.serposcope.db.google.GoogleDB;
import com.serphacker.serposcope.inteligenciaseo.SearchSettingsDB;
import com.serphacker.serposcope.models.base.Config;
import com.serphacker.serposcope.models.base.Group;
import com.serphacker.serposcope.models.base.Group.Module;
import com.serphacker.serposcope.models.base.Run;
import com.serphacker.serposcope.models.base.User;
import com.serphacker.serposcope.models.google.*;
import ninja.*;
import ninja.i18n.Messages;
import ninja.params.Param;
import ninja.params.PathParam;
import ninja.session.FlashScope;
import org.apache.commons.lang3.StringEscapeUtils;
import serposcope.filters.AdminFilter;
import serposcope.filters.XSRFFilter;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.serphacker.serposcope.db.base.RunDB.STATUSES_DONE;
import static com.serphacker.serposcope.models.google.GoogleRank.UNRANKED;


@Singleton
public class GoogleSearchController extends GoogleController {
    
    @Inject
    GoogleDB googleDB;

    @Inject
    SearchSettingsDB settingsDB;
    
    @Inject
    BaseDB baseDB;
    
    @Inject
    Router router;
    
    @Inject
    Messages msg;
    
    @Inject
    ObjectMapper objectMapper;

    @Inject
    SearchSettingsDB searchSettingsDB;


    public Result search(Context context, 
        @PathParam("searchId") Integer searchId,
        @Param("startDate") String startDateStr,
        @Param("endDate") String endDateStr
    ){
        GoogleSearch search = getSearch(context, searchId);
        Group group = context.getAttribute("group", Group.class);
        
        if(search == null){
            context.getFlashScope().error("error.invalidSearch");
            return Results.redirect(router.getReverseRoute(GoogleGroupController.class, "view", "groupId", group.getId()));
        }
        
        Run minRun = baseDB.run.findFirst(Module.GOOGLE, STATUSES_DONE, null);
        Run maxRun = baseDB.run.findLast(Module.GOOGLE, STATUSES_DONE, null);
        if(maxRun == null || minRun == null){
            return Results.ok()
                .render("search", search);
        }
        
        LocalDate minDay = minRun.getDay();
        LocalDate maxDay = maxRun.getDay();
        
        LocalDate startDate = null;
        if(startDateStr != null){
            try {startDate = LocalDate.parse(startDateStr);} catch(Exception ex){}
        }
        LocalDate endDate = null;
        if(endDateStr != null){
            try {endDate = LocalDate.parse(endDateStr);} catch(Exception ex){}
        }
        
        if(startDate == null || endDate == null || endDate.isBefore(startDate)){
            startDate = maxDay.minusDays(30);
            endDate = maxDay;
        }
        
        Run firstRun = baseDB.run.findFirst(Module.GOOGLE, STATUSES_DONE, startDate);
        Run lastRun = baseDB.run.findLast(Module.GOOGLE, STATUSES_DONE, endDate);
        
        if(firstRun == null || lastRun == null || firstRun.getDay().isAfter(lastRun.getDay())){
            return Results.ok()
                .render("f_warning", msg.get("error.noDataForThisPeriod", context, Optional.absent()).or(""))
                .render("startDate", startDate)
                .render("endDate", endDate)
                .render("minDate", minDay)
                .render("maxDate", maxDay)                        
                .render("search", search)
                .render("categories", settingsDB.getCategories(context.getAttribute("user", User.class)))
            ;
        }
        
        startDate = firstRun.getDay();
        endDate = lastRun.getDay();
        
        
        String jsonEvents = null;
        try {
            jsonEvents = objectMapper.writeValueAsString(baseDB.event.list(group, startDate, endDate));
        } catch(JsonProcessingException ex){
            jsonEvents = "[]";
        }
        
        GoogleSerp lastSerp = googleDB.serp.get(lastRun.getId(), search.getId());
        
        List<GoogleTarget> targets = getTargets(context);
        
        Map<Integer, GoogleBest> bestRankings = new HashMap<>();
        for (GoogleTarget target : targets) {
            GoogleBest best = googleDB.rank.getBest(target.getGroupId(), target.getId(), search.getId());
            if(best != null){
                bestRankings.put(best.getGoogleTargetId(), best);
            }
        }

        String jsonRanks = getJsonRanks(group, targets, firstRun, lastRun, searchId);
        Config config = baseDB.config.getConfig();

        User user = context.getAttribute("user", User.class);
        List<String> categories = settingsDB.getCategories(user);
        return Results.ok()
            .render("displayMode", config.getDisplayGoogleSearch())
            .render("events", user.isAdmin() ? jsonEvents : "[]")
            .render("targets", targets)
            .render("chart", jsonRanks)
            .render("search", search)
            .render("serp", lastSerp)
            .render("startDate", startDate)
            .render("endDate", endDate)
            .render("minDate", minDay)
            .render("maxDate", maxDay)
            .render("bestRankings", bestRankings)
            .render("categories", categories)
        ;
    }

    
    protected String getJsonRanks(Group group, List<GoogleTarget> targets,Run firstRun, Run lastRun, int searchId){
        
        StringBuilder builder = new StringBuilder("{\"targets\":[");
        for (GoogleTarget target : targets) {
            builder.append("{\"id\":").append(target.getId())
            .append(",\"name\":\"").append(StringEscapeUtils.escapeJson(target.getName())).append("\"},");
        }
        if(builder.charAt(builder.length()-1) == ','){
            builder.setCharAt(builder.length()-1, ']');
        } else {
            builder.append(']');
        }
        builder.append(",\"ranks\":[");
        
        final int[] maxRank = new int[1];
        
        googleDB.serp.stream(firstRun.getId(), lastRun.getId(), searchId, (GoogleSerp serp) -> {
            
            builder.append('[').append(serp.getRunDay().toEpochSecond(ZoneOffset.UTC)*1000l).append(',');
            
            // calendar
            builder.append("null").append(",");
            
            for (GoogleTarget target : targets) {
                int position = UNRANKED;
                for (int i = 0; i < serp.getEntries().size(); i++) {
                    if(target.match(serp.getEntries().get(i).getUrl())){
                        position = i + 1;
                        break;
                    }
                }
                
                builder.append(position == UNRANKED ? "null" : position).append(',');
                if(position != UNRANKED && position > maxRank[0]){
                    maxRank[0] = position;
                }
            }
            
            if(builder.charAt(builder.length()-1) == ','){
                builder.setCharAt(builder.length()-1, ']');
            }
            builder.append(',');
        });
        if(builder.charAt(builder.length()-1) == ','){
            builder.setCharAt(builder.length()-1, ']');
        } else {
            builder.append(']');
        }
        
        builder.append(",\"maxRank\":").append(maxRank[0]);
        builder.append("}");
        
        return builder.toString();
    }
    
    public Result urlRanks(
        Context context,
        @PathParam("searchId") Integer searchId,
        @Param("url") String url,
        @Param("startDate") String startDateStr,
        @Param("endDate") String endDateStr        
    ){
        Group group = (Group)context.getAttribute("group");
        
        GoogleSearch search = getSearch(context, searchId);
        if(search == null){
            context.getFlashScope().error("error.invalidSearch");
            return Results.redirect(router.getReverseRoute(GoogleGroupController.class, "view", "groupId", group.getId()));
        }        
        
        LocalDate startDate = null;
        if(startDateStr != null){
            try {startDate = LocalDate.parse(startDateStr);} catch(Exception ex){}
        }
        LocalDate endDate = null;
        if(endDateStr != null){
            try {endDate = LocalDate.parse(endDateStr);} catch(Exception ex){}
        }        
        
        Run firstRun = baseDB.run.findFirst(Module.GOOGLE, STATUSES_DONE, startDate);
        Run lastRun = baseDB.run.findLast(Module.GOOGLE, STATUSES_DONE, endDate);
        
        if(url == null || firstRun == null || lastRun == null){
            return Results.badRequest().text();
        }
        
        StringBuilder builder = new StringBuilder("{");
        googleDB.serp.stream(firstRun.getId(), lastRun.getId(), search.getId(), (GoogleSerp t) -> {
            int position = 0;
            for (int i = 0; i < t.getEntries().size(); i++) {
                if(t.getEntries().get(i).getUrl().equals(url)){
                    position = i + 1;
                    break;
                }
            }
            
            builder
                .append("\"")
                .append(t.getRunDay().toEpochSecond(ZoneOffset.UTC)*1000l)
                .append("\":")
                .append(position)
                .append(",");
        });
        
        if(builder.charAt(builder.length()-1) == ','){
            builder.setCharAt(builder.length()-1, '}');
        } else {
            builder.append('}');
        }
        
        return Results.ok()
            .text()
            .render(builder.toString());
    }
    
    public Result exportSerp(Context context, 
        @PathParam("searchId") Integer searchId,
        @Param("date") String pdate
    ){
        GoogleSerp serp=null;
        LocalDate date=null;
        try {date = LocalDate.parse(pdate);}catch(Exception ex){}
        if(date != null){
            List<Run> runs = baseDB.run.findByDay(Module.GOOGLE, date);
            if(!runs.isEmpty()){
                GoogleSearch search = getSearch(context, searchId);
                if(search != null){
                    serp = googleDB.serp.get(runs.get(0).getId(), search.getId());
                }
            }
        }
        
        if(serp == null){
            return Results.ok().text().renderRaw("SERP not found");
        }
        
        boolean exportRank = context.getParameter("rank") != null;
        boolean exportD1 = context.getParameter("d1") != null;
        boolean exportD7 = context.getParameter("d7") != null;
        boolean exportD30 = context.getParameter("d30") != null;
        boolean exportD90 = context.getParameter("d90") != null;
        
        int position = 0;
        StringBuilder builder = new StringBuilder();
        for (GoogleSerpEntry entry : serp.getEntries()) {
            ++position;
            if(exportRank){
                builder.append(position).append(",");
            }
            builder.append(StringEscapeUtils.escapeCsv(entry.getUrl())).append(",");
            if(exportD1){
                Short rank = entry.getMap().getOrDefault((short)1, (short)GoogleRank.UNRANKED);
                builder.append(rank != GoogleRank.UNRANKED ? rank.intValue() : "").append(",");
            }
            if(exportD7){
                Short rank = entry.getMap().getOrDefault((short)7, (short)GoogleRank.UNRANKED);
                builder.append(rank != GoogleRank.UNRANKED ? rank.intValue() : "").append(",");
            }
            if(exportD30){
                Short rank = entry.getMap().getOrDefault((short)30, (short)GoogleRank.UNRANKED);
                builder.append(rank != GoogleRank.UNRANKED ? rank.intValue() : "").append(",");
            }
            if(exportD90){
                Short rank = entry.getMap().getOrDefault((short)90, (short)GoogleRank.UNRANKED);
                builder.append(rank != GoogleRank.UNRANKED ? rank.intValue() : "").append(",");
            }
            if(builder.length() > 0){
                builder.setCharAt(builder.length()-1, '\n');
            }
        }
        
        return Results.text()
            .addHeader("Content-Disposition", "attachment; filename=\"" + serp.getRunDay().toLocalDate() + ".csv\"")
            .renderRaw(builder.toString());
    }    

    @FilterWith({
            XSRFFilter.class,
            AdminFilter.class
    })
     public Result setVisibility(Context context,
                                @Param("id") Integer searchId,
                                @Param("visible") Boolean visible)
    {
        FlashScope flash = context.getFlashScope();
        Group group = context.getAttribute("group", Group.class);
        if (searchSettingsDB.setVisibleForAll(searchId, group.getId(), visible)) {
            flash.success("inteligenciaseo.visibilityChanged");
            return Results.redirect(router.getReverseRoute(GoogleGroupController.class, "view", "groupId", group.getId()) + "#tab-searches");
        } else {
            flash.error("error.changeVisibilityFailed");
            return Results.text();
        }
    }

}