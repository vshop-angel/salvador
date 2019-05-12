package serposcope.controllers.admin;

import com.google.common.base.Optional;
import com.serphacker.serposcope.db.google.GoogleRankDB;
import com.serphacker.serposcope.db.google.GoogleSearchDB;
import com.serphacker.serposcope.db.google.GoogleSerpDB;
import com.serphacker.serposcope.inteligenciaseo.InactiveKeywordsDB;
import ninja.*;
import ninja.i18n.Messages;
import ninja.session.FlashScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import serposcope.controllers.BaseController;
import serposcope.filters.AdminFilter;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.List;

@SuppressWarnings("Guava")
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
    private Messages messages;

    @Inject
    HouseKeepingController(Messages messages) {
        this.messages = messages;
    }

    private Result removeKeywordList(Context context, List<Integer> list) {
        FlashScope flash = context.getFlashScope();
        Optional<String> language = Optional.of("es");
        InactiveKeywordsDB.Deleter<Result> deleter = new InactiveKeywordsDB.Deleter<Result>() {
            @Override
            public Result onAlreadyClean() {
                flash.success("message.yourHouseIsClean");
                return Results.redirect(router.getReverseRoute(AdminController.class, "admin"));
            }

            @Override
            public Result onError(int count) {
                Object[] parameters = {Integer.toString(count)};
                Optional<String> message = messages.get("message.noKeywordsDeleted", language, parameters);
                // Display success message
                if (message.isPresent()) {
                    flash.success(message.get());
                }
                return Results.redirect(router.getReverseRoute(AdminController.class, "admin"));
            }

            @Override
            public Result onPartialSuccess(int actualCount, int expectedCount) {
                Object[] parameters = {Integer.toString(actualCount), Integer.toString(expectedCount)};
                Optional<String> message =  messages.get("message.someKeywordsDeleted", language, parameters);
                // Display success message
                if (message.isPresent()) {
                    flash.success(message.get());
                }
                return Results.redirect(router.getReverseRoute(AdminController.class, "admin"));
            }

            @Override
            public Result onSuccess(int count) {
                Object[] parameters = {Integer.toString(count)};
                Optional<String> message = messages.get("message.allKeywordsDeleted", language, parameters);
                // Display success message
                if (message.isPresent()) {
                    flash.success(message.get());
                }
                return Results.redirect(router.getReverseRoute(AdminController.class, "admin"));
            }
        };
        return inactiveKeywordsDB.removeFromList(list, deleter);
    }

    public Result removeInactiveKeywords(Context context) {
        List<Integer> list = inactiveKeywordsDB.getInactiveSearchIds();
        return removeKeywordList(context, list);
    }
}
