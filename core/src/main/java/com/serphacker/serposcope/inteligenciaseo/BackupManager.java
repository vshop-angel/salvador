package com.serphacker.serposcope.inteligenciaseo;

import com.serphacker.serposcope.db.base.ExportDB;

import javax.inject.Singleton;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.sql.Time;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPOutputStream;

@Singleton
public class BackupManager {
    private static final long OnekB = 1024L;
    private static final long OneMB = 1024L * OnekB;
    private static final long OneGB = 1024L * OneMB;

    private Thread thread;
    private String currentFile;

    public enum Status {
        Pending, Finished
    }

    public static class BackupEntry {
        public String name;
        public Date date;
        public Time time;
        public String size;
        public Status status;
    }

    public static String getTargetPath() {
        return System.getProperty("user.home");
    }

    public synchronized boolean isBusy() {
        return thread != null && thread.isAlive();
    }

    public synchronized Status getStatusFor(String name) {
        if (name.equals(currentFile)) {
            return Status.Pending;
        } else {
            return Status.Finished;
        }
    }

    public String getSizeOf(String name) {
        File file = new File(String.format("%s/%s", getTargetPath(), name));
        return humanizedSize(file.length());
    }

    private void removeExpiredFile() {
        list = list();
    }

    public void create(ExportDB exportDB) {
        if (isBusy())
            return;
        // Cleanup
        removeExpiredFile();
        // Start creating
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy.MM.dd-HH.mm");
        // We set this as we are processing the file
        currentFile = String.format("backup-%s.sql.gz", now.format(formatter));
        // Build the path
        String path = String.format("%s/%s", getTargetPath(), currentFile);
        // Create a thread to run this in the background
        thread = new Thread(() -> {
            try (
                    FileOutputStream fileStream = new FileOutputStream(new File(path));
                    GZIPOutputStream gzos = new GZIPOutputStream(fileStream);
                    OutputStreamWriter osw = new OutputStreamWriter(gzos, StandardCharsets.UTF_8);
                    Writer writer = new PrintWriter(osw)
            ) {
                exportDB.export(writer);
                // Modify data from above?
                currentFile = null;
                thread = null;
            } catch (IOException ignored) {
            }
        });
        thread.start();
    }

    private static String humanizedSize(long value) {
        if (value < OnekB) {
            return String.format("%dB", value);
        } else if (value < OneMB) {
            return String.format("%dkB", value / OnekB);
        } else if (value < OneGB) {
            return String.format("%dMB", value / OneMB);
        } else {
            return "?";
        }
    }

    public List<BackupEntry> list() {
        List<BackupEntry> list = new ArrayList<>();
        File file = new File(getTargetPath());
        File[] files = file.listFiles();
        if (files == null) {
            return list;
        }
        for (File each : files) {
            BackupEntry entry = new BackupEntry();
            try {
                BasicFileAttributes attrs = Files.readAttributes(Paths.get(each.getAbsolutePath()), BasicFileAttributes.class);
                String name = each.getName();
                if (!name.startsWith("backup-") || !name.endsWith(".sql.gz"))
                    continue;
                FileTime ctime = attrs.creationTime();
                Date date = new Date(ctime.to(TimeUnit.MILLISECONDS));

                entry.name = name;
                entry.size = humanizedSize(each.length());
                entry.date = date;
                entry.status = getStatusFor(name);
                entry.time = new Time(date.getTime());

                list.add(entry);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return list;
    }
}
