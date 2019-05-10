/* 
 * Serposcope - SEO rank checker https://serposcope.serphacker.com/
 * 
 * Copyright (c) 2016 SERP Hacker
 * @author Pierre Nogues <support@serphacker.com>
 * @license https://opensource.org/licenses/MIT MIT License
 */
package conf;


import ninja.AssetsController;
import ninja.Router;
import ninja.application.ApplicationRoutes;
import serposcope.controllers.AuthController;
import serposcope.controllers.GroupController;
import serposcope.controllers.HomeController;
import serposcope.controllers.UserPreferences;
import serposcope.controllers.admin.*;
import serposcope.controllers.google.GoogleGroupController;
import serposcope.controllers.google.GoogleSearchController;
import serposcope.controllers.google.GoogleTargetController;
import serposcope.controllers.inteligenciaseo.ReportsController;

public class Routes implements ApplicationRoutes {

    @Override
    public void init(Router router) {  
        
        // authentication
        router.GET().route("/create-admin").with(AuthController.class, "createAdmin");
        router.POST().route("/create-admin").with(AuthController.class, "doCreateAdmin");
        router.GET().route("/login").with(AuthController.class, "login");
        router.POST().route("/login").with(AuthController.class, "doLogin");
        router.GET().route("/logout").with(AuthController.class, "logout");        
        
        // admin
        router.GET().route("/admin").with(AdminController.class, "admin");
        router.GET().route("/admin/sysconfig").with(AdminController.class, "sysconfig");
        router.GET().route("/admin/stackdump").with(AdminController.class, "stackdump");
        router.GET().route("/admin/sql/export").with(AdminController.class, "exportSQL");
        router.POST().route("/admin/sql/import").with(AdminController.class, "importSQL");
        
        router.GET().route("/admin/debug").with(DebugController.class, "debug");
        router.POST().route("/admin/debug/wipe-rankings").with(DebugController.class, "wipeRankings");
        router.POST().route("/admin/debug/wipe-groups").with(DebugController.class, "wipeGroups");
        router.POST().route("/admin/debug/generate").with(DebugController.class, "generate");
        router.POST().route("/admin/debug/dry-run").with(DebugController.class, "dryRun"); 
        router.GET().route("/admin/debug/test").with(DebugController.class, "test"); 
        router.GET().route("/admin/debug/shutdown").with(DebugController.class, "shutdown"); 
        router.POST().route("/admin/debug/dummy-post").with(DebugController.class, "dummyPost");

        router.GET().route("/admin/backup").with(BackupController.class, "table");
        router.GET().route("/admin/backup/create").with(BackupController.class, "create");
        router.GET().route("/admin/backup/download").with(BackupController.class, "download");
        router.GET().route("/admin/backup/status").with(BackupController.class, "getStatus");
        router.POST().route("/admin/backup/delete").with(BackupController.class, "delete");
        router.POST().route("/admin/backup/restore").with(BackupController.class, "restore");

        router.GET().route("/admin/housekeeping/remove-inactive-keywords").with(HouseKeepingController.class, "removeInactiveKeywords");

        router.GET().route("/admin/settings").with(SettingsController.class, "settings");
        router.POST().route("/admin/settings/update").with(SettingsController.class, "update");
        router.POST().route("/admin/settings/reset").with(SettingsController.class, "reset");        
        router.GET().route("/admin/settings/test-captcha").with(SettingsController.class, "testCaptcha");
        router.POST().route("/admin/settings/prune").with(SettingsController.class, "prune");
        
        router.GET().route("/admin/google").with(GoogleSettingsController.class, "settings");
        router.POST().route("/admin/google/update").with(GoogleSettingsController.class, "update");
        router.POST().route("/admin/google/reset").with(GoogleSettingsController.class, "reset");
        
        router.GET().route("/admin/users").with(UsersController.class, "users");
        router.POST().route("/admin/users/add").with(UsersController.class, "add");
        router.POST().route("/admin/users/delete").with(UsersController.class, "delete");
        router.POST().route("/admin/users/permissions/set").with(UsersController.class, "setPerm");        
        
        router.GET().route("/admin/tasks").with(TaskController.class, "tasks");
        router.GET().route("/admin/tasks/start").with(TaskController.class, "startTask");
        router.GET().route("/admin/tasks/stop").with(TaskController.class, "abortTask");
        router.POST().route("/admin/tasks/{runId: [0-9]+}/delete").with(TaskController.class, "deleteRun");
        router.POST().route("/admin/tasks/{runId: [0-9]+}/rescan-serp").with(TaskController.class, "rescanSerp");
        // Execute task for a single keyword

        router.GET().route("/admin/logs").with(LogController.class, "logs");
        router.GET().route("/admin/logs/view").with(LogController.class, "viewLog");
        
        router.GET().route("/admin/proxies").with(ProxyController.class, "proxies");
        router.POST().route("/admin/proxies/add").with(ProxyController.class, "add");
        router.POST().route("/admin/proxies/delete").with(ProxyController.class, "delete");
        router.POST().route("/admin/proxies/delete-invalid").with(ProxyController.class, "deleteInvalid");
        router.POST().route("/admin/proxies/check").with(ProxyController.class, "startCheck");
        router.POST().route("/admin/proxies/abort").with(ProxyController.class, "abortCheck");
        
        // home / group
        router.GET().route("/").with(HomeController.class, "home");
        router.GET().route("/task-status/{taskId: [0-9]+}").with(HomeController.class, "taskStatus");
        
        router.GET().route("/groups").with(GroupController.class, "groups");
        router.POST().route("/groups/create").with(GroupController.class, "create");
        router.GET().route("/groups/suggest").with(GroupController.class, "jsonSuggest");
        
        router.GET().route("/preferences").with(UserPreferences.class, "preferences");
        router.POST().route("/preferences/update").with(UserPreferences.class, "update");
        
        // google
        router.GET().route("/google/{groupId: [0-9]+}").with(GoogleGroupController.class, "view");
        router.POST().route("/google/{groupId: [0-9]+}/rename").with(GoogleGroupController.class, "rename");
        router.POST().route("/google/{groupId: [0-9]+}/delete").with(GoogleGroupController.class, "delete");        
        router.POST().route("/google/{groupId: [0-9]+}/search/add").with(GoogleGroupController.class, "addSearch");
        router.POST().route("/google/{groupId: [0-9]+}/search/delete").with(GoogleGroupController.class, "delSearch");
        router.POST().route("/google/{groupId: [0-9]+}/search/export-searches").with(GoogleGroupController.class, "exportSearches");
        router.GET().route("/google/{groupId: [0-9]+}/search/suggest").with(GoogleGroupController.class, "jsonSearchSuggest");
        router.GET().route("/google/{groupId: [0-9]+}/search/list").with(GoogleGroupController.class, "jsonSearches");
        
        router.POST().route("/google/{groupId: [0-9]+}/target/add").with(GoogleGroupController.class, "addTarget");
        router.POST().route("/google/{groupId: [0-9]+}/report/add").with(GoogleGroupController.class, "addReport");
        router.POST().route("/google/{groupId: [0-9]+}/target/delete").with(GoogleGroupController.class, "delTarget");
        router.POST().route("/google/{groupId: [0-9]+}/target/rename").with(GoogleGroupController.class, "renameTarget");
        router.GET().route("/google/{groupId: [0-9]+}/target/suggest").with(GoogleGroupController.class, "jsonTargetSuggest");
        router.GET().route("/google/{groupId: [0-9]+}/report/suggest").with(GoogleGroupController.class, "jsonReportSuggest");
        router.GET().route("/google/report/{reportId: [0-9]+}").with(ReportsController.class, "get");
        router.POST().route("/google/{groupId: [0-9]+}/report/delete").with(GoogleGroupController.class, "deleteReport");

        router.POST().route("/google/{groupId: [0-9]+}/event/add").with(GoogleGroupController.class, "addEvent");
        router.POST().route("/google/{groupId: [0-9]+}/event/delete").with(GoogleGroupController.class, "delEvent");   
        
        router.GET().route("/google/{groupId: [0-9]+}/search/{searchId: [0-9]+}").with(GoogleSearchController.class, "search");
        router.GET().route("/google/{groupId: [0-9]+}/search/{searchId: [0-9]+}/url-ranks").with(GoogleSearchController.class, "urlRanks");
        router.GET().route("/google/{groupId: [0-9]+}/search/{searchId: [0-9]+}/export-serp").with(GoogleSearchController.class, "exportSerp");
        router.POST().route("/google/{groupId: [0-9]+}/search/{searchId: [0-9]+}/set-volume").with(GoogleSearchController.class, "setVolume");
        router.POST().route("/google/{groupId: [0-9]+}/search/{searchId: [0-9]+}/set-cpc").with(GoogleSearchController.class, "setCPC");
        router.POST().route("/google/{groupId: [0-9]+}/search/{searchId: [0-9]+}/set-competition").with(GoogleSearchController.class, "setCompetition");
        router.POST().route("/google/{groupId: [0-9]+}/search/{searchId: [0-9]+}/set-tag").with(GoogleSearchController.class, "setTag");
        router.POST().route("/google/{groupId: [0-9]+}/search/{searchId: [0-9]+}/set-category").with(GoogleSearchController.class, "setCategory");

        router.GET().route("/google/{groupId: [0-9]+}/target/{targetId: [0-9]+}/ranks").with(GoogleTargetController.class, "jsonRanks");
        router.GET().route("/google/{groupId: [0-9]+}/target/{targetId: [0-9]+}/variation").with(GoogleTargetController.class, "jsonVariation");
        router.GET().route("/google/{groupId: [0-9]+}/target/{targetId: [0-9]+}").with(GoogleTargetController.class, "target");
        router.POST().route("/google/{groupId: [0-9]+}/search/set-visibility").with(GoogleSearchController.class, "setVisibility");
        
        router.GET().route("/assets/{fileName: .*}").with(AssetsController.class, "serveStatic");
    }

}
