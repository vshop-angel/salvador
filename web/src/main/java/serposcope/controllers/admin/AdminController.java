/* 
 * Serposcope - SEO rank checker https://serposcope.serphacker.com/
 * 
 * Copyright (c) 2016 SERP Hacker
 * @author Pierre Nogues <support@serphacker.com>
 * @license https://opensource.org/licenses/MIT MIT License
 */
package serposcope.controllers.admin;

import javax.inject.Inject;
import javax.inject.Singleton;
import com.serphacker.serposcope.db.base.ExportDB;
import conf.SerposcopeConf;
import ninja.*;
import ninja.params.Param;
import ninja.session.FlashScope;
import ninja.uploads.DiskFileItemProvider;
import ninja.uploads.FileItem;
import ninja.uploads.FileProvider;
import ninja.utils.ResponseStreams;
import org.apache.commons.fileupload.FileUploadException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import serposcope.controllers.BaseController;
import serposcope.filters.AdminFilter;
import serposcope.filters.XSRFFilter;

import java.io.*;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Properties;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

@FilterWith(AdminFilter.class)
@Singleton
public class AdminController extends BaseController {

    private static final Logger LOG = LoggerFactory.getLogger(AdminController.class);

    @Inject
    SerposcopeConf conf;
    
    @Inject
    Router router;

    @Inject
    ExportDB exportDB;

    public Result admin() {
        return Results
            .ok();
    }

    public Result sysconfig() {

        StringBuilder builder = new StringBuilder(conf.dumpEnv());

        Properties props = System.getProperties();
        for (Map.Entry<Object, Object> entry : props.entrySet()) {
            builder.append(entry.getKey()).append("=").append(entry.getValue()).append("\n");
        }

        return Results
            .ok()
            .text()
            .render(builder.toString());
    }

    public Result stackdump(Context context) {

        return Results
            .contentType("text/plain")
            .render((ctx, res) -> {
                ResponseStreams responseStreams = context.finalizeHeaders(res);
                try (
                    PrintWriter writer = new PrintWriter(responseStreams.getOutputStream());) {
                    final ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
                    final ThreadInfo[] threadInfos = threadMXBean.getThreadInfo(threadMXBean.getAllThreadIds(), 100);
                    for (ThreadInfo threadInfo : threadInfos) {
                        writer.append('"');
                        writer.append(threadInfo.getThreadName());
                        writer.append("\" ");
                        final Thread.State state = threadInfo.getThreadState();
                        writer.append("\n   java.lang.Thread.State: ");
                        writer.append(state.toString());
                        final StackTraceElement[] stackTraceElements = threadInfo.getStackTrace();
                        for (final StackTraceElement stackTraceElement : stackTraceElements) {
                            writer.append("\n        at ");
                            writer.append(stackTraceElement.toString());
                        }
                        writer.println("\n");
                    }
                } catch (IOException ex) {
                    LOG.error("stackdump", ex);
                }
            });
    }

    @FilterWith(XSRFFilter.class)
    public Result exportSQL(Context context) {
        return Results
            .contentType("application/octet-stream")
            .addHeader("Content-Disposition", "attachment; filename=\"export-utf8.sql.gz\"")
            .render((ctx, res) -> {
                ResponseStreams responseStreams = context.finalizeHeaders(res);
                try (
                    GZIPOutputStream gzos = new GZIPOutputStream(responseStreams.getOutputStream());
                    OutputStreamWriter osw = new OutputStreamWriter(gzos, StandardCharsets.UTF_8);
                    Writer writer = new PrintWriter(osw);
                ) {
                    exportDB.export(writer);
                } catch (IOException ex) {
                    LOG.error("export dl ex", ex);
                }
            });
    }
    
    @FileProvider(DiskFileItemProvider.class)
    @FilterWith(XSRFFilter.class)
    public Result importSQL(Context context, @Param("dump") FileItem fileItem) throws FileUploadException, IOException {
        FlashScope flash = context.getFlashScope();
        
        if(fileItem == null){
            flash.error("error.noFileUploaded");
            return Results.redirect(router.getReverseRoute(AdminController.class, "admin"));
        }
        
        try {
            InputStream is = fileItem.getInputStream();
            if(fileItem.getFileName().endsWith(".gz")){
                is = new GZIPInputStream(is);
            }

            try(BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))){
                exportDB.importStream(reader);
            }
        }catch(Exception ex){
            LOG.error("SQL import error", ex);
            flash.error("error.internalError");
            return Results.redirect(router.getReverseRoute(AdminController.class, "admin"));
        }
        
        flash.success("admin.menu.importSuccessful");
        return Results.redirect(router.getReverseRoute(AdminController.class, "admin"));
    }    

}
