package serposcope.controllers.admin;

import com.serphacker.serposcope.db.base.ExportDB;
import com.serphacker.serposcope.inteligenciaseo.BackupManager;
import ninja.*;
import ninja.params.Param;
import ninja.params.Params;
import ninja.session.FlashScope;
import ninja.utils.ResponseStreams;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import serposcope.controllers.BaseController;
import serposcope.filters.AdminFilter;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@FilterWith(AdminFilter.class)
@Singleton
public class BackupController extends BaseController {
    @Inject
    private ExportDB exportDB;

    @Inject
    private Router router;

    @Inject
    private BackupManager backupManager;

    private static final Logger LOG = LoggerFactory.getLogger(BackupController.class);

    public Result table() {
        return Results
                .ok()
                .render("files", backupManager.list())
                .render("status", backupManager.isBusy() ? "disabled" : "data-ignore")
                ;
    }

    public Result getStatus(@Param("name") String name) {
        BackupManager.Status status = backupManager.getStatusFor(name);
        Map<String, String> data = new HashMap<>();
        data.put("size", backupManager.getSizeOf(name));
        data.put("status", status.name());
        return Results
                .json()
                .render(data)
                ;
    }

    public Result download(@Param("name") String name) {
        String path = String.format("%s/%s", BackupManager.getTargetPath(), name);
        return Results
                .contentType("application/octet-stream")
                .addHeader("Content-Disposition", String.format("attachment; filename=\"%s\"", name))
                .render((ctx, res) -> {
                    ResponseStreams responseStreams = ctx.finalizeHeaders(res);
                    try (FileInputStream inputStream = new FileInputStream(path)) {
                        IOUtils.copy(inputStream, responseStreams.getOutputStream());
                    } catch (IOException e) {
                        LOG.error("cannot copy streams", e);
                    }
                })
                ;
    }

    public Result create(Context context) {
        FlashScope flash = context.getFlashScope();
        try {
            backupManager.create(exportDB);
            flash.success("message.startedBackupCreation");
        } catch (Exception e) {
            LOG.info("Exception", e);
            flash.error("error.creatingBackup");
        }
        return Results.redirect(router.getReverseRoute(BackupController.class, "table"));
    }

    public Result delete(Context context, @Params("names[]") String[] names) {
        FlashScope flash = context.getFlashScope();
        boolean error = false;
        for (String name : names) {
            if (!backupManager.deleteOne(name)) {
                LOG.info(String.format("error deleting: '%s'", name));
                error = true;
            }
        }
        if (!error) {
            flash.success("message.deleteBackupSuccess");
        } else {
            flash.error("error.deleteBackupFailed");
        }
        return Results.redirect(router.getReverseRoute(BackupController.class, "table"));
    }

    public Result restore(Context context, @Params("names[]") String[] names) {
        FlashScope flash = context.getFlashScope();
        try {
            backupManager.restore(names[0], exportDB);
            flash.success("message.importBackupSuccess");
        } catch (Exception e) {
            LOG.error("Import", e);
            flash.error("error.importBackupFailed");
        }
        return Results.redirect(router.getReverseRoute(BackupController.class, "table"));
    }
}
