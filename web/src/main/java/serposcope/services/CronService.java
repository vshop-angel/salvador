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
import com.serphacker.serposcope.inteligenciaseo.BackupManager;
import com.serphacker.serposcope.models.base.Config;
import com.serphacker.serposcope.models.base.Group.Module;
import com.serphacker.serposcope.models.base.Run;
import com.serphacker.serposcope.task.TaskManager;
import ninja.lifecycle.Dispose;
import ninja.lifecycle.Start;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

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

    @Inject
    private BackupManager backupManager;
    
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
        backupManager.create(exportDB);
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
