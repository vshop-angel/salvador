/*
 * Serposcope - SEO rank checker https://serposcope.serphacker.com/
 * 
 * Copyright (c) 2016 SERP Hacker
 * @author Pierre Nogues <support@serphacker.com>
 * @license https://opensource.org/licenses/MIT MIT License
 */

package serposcope.services;

import com.serphacker.serposcope.db.base.ConfigDB;
import com.serphacker.serposcope.db.base.ExportDB;
import com.serphacker.serposcope.db.base.PruneDB;
import com.serphacker.serposcope.models.base.Config;
import com.serphacker.serposcope.models.base.Group;
import com.serphacker.serposcope.models.base.Group.Module;
import com.serphacker.serposcope.models.base.Run;
import com.serphacker.serposcope.task.TaskManager;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;
import javax.inject.Singleton;
import ninja.lifecycle.Dispose;
import ninja.lifecycle.Start;
import ninja.scheduler.Schedule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sun.java2d.pipe.SpanShapeRenderer;

@Singleton
public class CronService implements Runnable {
    
    private static final Logger LOG = LoggerFactory.getLogger(CronService.class);
    
    LocalTime previousCheck = null;
    ScheduledExecutorService executor;

    
    @Inject
    TaskManager manager;
    
    @Inject
    ConfigDB configDB;
    
    @Inject
    PruneDB pruneDB;

    @Inject
    ExportDB exportDB;
    
    @Start(order = 90)
    public void startService() {
        LOG.info("startService");
        executor = Executors.newSingleThreadScheduledExecutor();
        executor.scheduleAtFixedRate(this,0, 30, TimeUnit.SECONDS);
    }

    @Dispose(order = 90)
    public void stopService() {
       LOG.info("stopService");
       try{executor.shutdownNow();}catch(Exception ex){}
    }

    private boolean isReadyToRun(LocalTime time, LocalTime now) {
        if (time == null) {
            return false;
        }
        return time.getHour() == now.getHour() && time.getMinute() == now.getMinute();
    }

    private void runGoogleTask(int pruneRuns) {
        if (manager.startGoogleTask(new Run(Run.Mode.CRON, Module.GOOGLE, LocalDateTime.now()))) {
            LOG.debug("starting google task via cron");
        } else {
            LOG.debug("failed to start google task via cron, this task is already running");
            return;
        }

        try {
            manager.joinGoogleTask();
        } catch (InterruptedException ex) {
            LOG.debug("interrupted while waiting for google task");
            return;
        }

        if (pruneRuns > 0) {
            long pruned = pruneDB.prune(pruneRuns);
            LOG.info("history pruning : {} runs deleted", pruned);
        } else {
            LOG.info("history pruning is disabled");
        }
    }

    private void createBackup() {
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy.MM.dd-HH.mm");
        String path = String.format("%s/%s.sql.gz", System.getProperty("user.home"), now.format(formatter));
        try {
            exportDB.export(path);
        } catch (Exception ex) {
            LOG.error("ExportError", ex);
        }
    }

    @Override
    public void run() {
        LocalTime now = LocalTime.now();
        LocalTime next;
        if (previousCheck != null && now.getMinute() == previousCheck.getMinute()) {
            // What a stupid condition, wonder how is this implemented?
            return;
        }
        previousCheck = now;
        // Get app's config
        Config config = configDB.getConfig();
        // First check for the `cron' scanner
        next = config.getCronTime();
        if (isReadyToRun(next, now)) {
            runGoogleTask(config.getPruneRuns());
        }
        next = config.getBackupTime();
        if (isReadyToRun(next, now)) {
            createBackup();
        }
    }
}
