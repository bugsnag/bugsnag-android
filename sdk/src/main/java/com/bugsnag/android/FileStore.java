package com.bugsnag.android;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.facebook.infer.annotation.ThreadSafe;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@ThreadSafe
abstract class FileStore<T extends JsonStream.Streamable> {

    @NonNull
    protected final Configuration config;
    @Nullable
    final String storeDirectory;
    private final int maxStoreCount;
    private final Comparator<File> comparator;

    final Lock lock = new ReentrantLock();
    final Collection<File> queuedFiles = new ConcurrentSkipListSet<>();

    FileStore(@NonNull Configuration config, @NonNull Context appContext, String folder,
              int maxStoreCount, Comparator<File> comparator) {
        this.config = config;
        this.maxStoreCount = maxStoreCount;
        this.comparator = comparator;

        String path;
        try {
            path = appContext.getCacheDir().getAbsolutePath() + folder;

            File outFile = new File(path);
            outFile.mkdirs();
            if (!outFile.exists()) {
                Logger.warn("Could not prepare file storage directory");
                path = null;
            }
        } catch (Exception exception) {
            Logger.warn("Could not prepare file storage directory", exception);
            path = null;
        }
        this.storeDirectory = path;
    }

    @Nullable
    String write(@NonNull T streamable) {
        if (storeDirectory == null) {
            return null;
        }

        // Limit number of saved errors to prevent disk space issues
        File exceptionDir = new File(storeDirectory);
        if (exceptionDir.isDirectory()) {
            File[] files = exceptionDir.listFiles();

            if (files != null && files.length >= maxStoreCount) {
                // Sort files then delete the first one (oldest timestamp)
                Arrays.sort(files, comparator);

                for (int k = 0; k < files.length && files.length >= maxStoreCount; k++) {
                    File oldestFile = files[k];

                    if (!queuedFiles.contains(oldestFile)) {
                        Logger.warn(String.format("Discarding oldest error as stored "
                            + "error limit reached (%s)", oldestFile.getPath()));
                        deleteStoredFiles(Collections.singleton(oldestFile));
                    }
                }
            }
        }

        String filename = getFilename(streamable);

        JsonStream stream = null;
        lock.lock();

        try {
            FileOutputStream fos = new FileOutputStream(filename);
            Writer out = new BufferedWriter(new OutputStreamWriter(fos, "UTF-8"));
            stream = new JsonStream(out);
            stream.value(streamable);
            Logger.info(String.format("Saved unsent payload to disk (%s) ", filename));
            return filename;
        } catch (Exception exception) {
            Logger.warn(String.format("Couldn't save unsent payload to disk (%s) ",
                filename), exception);
        } finally {
            IOUtils.closeQuietly(stream);
            lock.unlock();
        }
        return null;
    }

    @NonNull
    abstract String getFilename(T streamable);

    List<File> findStoredFiles() {
        lock.lock();
        try {
            List<File> files = new ArrayList<>();

            if (storeDirectory != null) {
                File dir = new File(storeDirectory);

                if (dir.exists() && dir.isDirectory()) {
                    File[] values = dir.listFiles();

                    if (values != null) {
                        for (File value : values) {
                            if (value.isFile() && !queuedFiles.contains(value)) {
                                files.add(value);
                            }
                        }
                    }
                }
            }
            queuedFiles.addAll(files);
            return files;
        } finally {
            lock.unlock();
        }
    }

    void cancelQueuedFiles(Collection<File> files) {
        lock.lock();
        try {
            if (files != null) {
                queuedFiles.removeAll(files);
            }
        } finally {
            lock.unlock();
        }
    }

    void deleteStoredFiles(Collection<File> storedFiles) {
        lock.lock();
        try {
            if (storedFiles != null) {
                queuedFiles.removeAll(storedFiles);

                for (File storedFile : storedFiles) {
                    if (!storedFile.delete()) {
                        storedFile.deleteOnExit();
                    }
                }
            }
        } finally {
            lock.unlock();
        }
    }

}
