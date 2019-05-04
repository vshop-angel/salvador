package serposcope.controllers.admin;

import com.serphacker.serposcope.db.google.GoogleRankDB;
import com.serphacker.serposcope.db.google.GoogleSearchDB;
import com.serphacker.serposcope.db.google.GoogleSerpDB;
import com.serphacker.serposcope.inteligenciaseo.InactiveKeywordsDB;
import com.serphacker.serposcope.models.google.GoogleSearch;
import ninja.*;
import ninja.session.FlashScope;
import org.omg.CORBA.INTERNAL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import serposcope.controllers.BaseController;
import serposcope.filters.AdminFilter;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.List;

@FilterWith(AdminFilter.class)
@Singleton
public class HouseKeepingController extends BaseController {
    @Inject
    private Router router;

    @Inject
    private InactiveKeywordsDB inactiveKeywordsDB;

    @Inject
    private GoogleSearchDB searchDB;

    @Inject
    private GoogleSerpDB serpDB;

    @Inject
    private GoogleRankDB rankDB;

    private static final Logger LOG = LoggerFactory.getLogger(BackupController.class);

    public Result removeInactiveKeywords(Context context) {
        FlashScope flash = context.getFlashScope();
        List<Integer> list = inactiveKeywordsDB.getInactiveSearchIds();
        if (list.size() == 0) {
            flash.success("message.yourHouseIsClean");
            return Results.redirect(router.getReverseRoute(AdminController.class, "admin"));
        }
        int count = 0;
        for (Integer id: list) {
            GoogleSearch search = searchDB.find(id);
            if (search == null) {
                LOG.warn(String.format("Could not find search with id %d", id));
                continue;
            }
            // Delete the serp first
            serpDB.deleteBySearch(id);
            // Get all groups
            List<Integer> groups = searchDB.listGroups(search);
            for (Integer group: groups) {
                // Remove the ranks saved for this search
                rankDB.deleteBySearch(group, search.getId());
                // Remove the search from the group
                searchDB.deleteFromGroup(search, group);
            }
            // Now attempt to delete the search itself (it will probably fail)
            if (searchDB.delete(search)) {
                count += 1;
            }
        }
        if (count == 0) {
            flash.error("error.noKeywordsDeleted");
        } else if (count == list.size()) {
            flash.success("message.allKeywordsDeleted");
        } else {
            flash.success("message.someKeywordsDeleted");
        }
        return Results.redirect(router.getReverseRoute(AdminController.class, "admin"));
    }
}
