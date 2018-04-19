package com.bugsnag.android;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.io.File;
import java.io.FileWriter;
import java.io.Writer;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

abstract class FileStore<T extends JsonStream.Streamable> {

    @NonNull
    protected final Configuration config;

    @Nullable
    final String oldDirectory;

    File storageDir;
    private final int maxStoreCount;
    private final Comparator<File> comparator;

    FileStore(@NonNull Configuration config, @NonNull Context appContext, String folder,
              int maxStoreCount, Comparator<File> comparator) {
        this.config = config;
        this.maxStoreCount = maxStoreCount;
        this.comparator = comparator;

        String path;
        try {
            File baseDir = new File(appContext.getCacheDir().getAbsolutePath(), folder);
            path = baseDir.getAbsolutePath();
            storageDir = getStorageDir(path, config);

            if (!storageDir.exists()) {
                Logger.warn("Could not prepare file storage directory");
                path = null;
            }
        } catch (Exception exception) {
            Logger.warn("Could not prepare file storage directory", exception);
            path = null;
        }
        this.oldDirectory = path;
    }

    @Nullable
    String write(@NonNull T streamable) {
        if (storageDir == null) {
            return null;
        }

        // Limit number of saved errors to prevent disk space issues
        if (storageDir.isDirectory()) {
            File[] files = storageDir.listFiles();
            if (files != null && files.length >= maxStoreCount) {
                // Sort files then delete the first one (oldest timestamp)
                Arrays.sort(files, comparator);
                Logger.warn(String.format("Discarding oldest error as stored "
                    + "error limit reached (%s)", files[0].getPath()));
                if (!files[0].delete()) {
                    files[0].deleteOnExit();
                }
            }
        }

        String filename = getFilename(streamable);

        Writer out = null;
        try {
            out = new FileWriter(filename);

            JsonStream stream = new JsonStream(out);
            stream.value(streamable);
            stream.close();

            Logger.info(String.format("Saved unsent payload to disk (%s) ", filename));
            return filename;
        } catch (Exception exception) {
            Logger.warn(String.format("Couldn't save unsent payload to disk (%s) ",
                filename), exception);
        } finally {
            IOUtils.closeQuietly(out);
        }
        return null;
    }

    @NonNull
    abstract String getFilename(T streamable);

    List<File> findStoredFiles() {
        List<File> files = new ArrayList<>();

        if (oldDirectory != null) {
            File dir = new File(oldDirectory);
            addStoredFiles(dir, files);
        }
        if (storageDir != null) {
            addStoredFiles(storageDir, files);
        }
        return files;
    }

    void addStoredFiles(File dir, List<File> files) {
        if (!dir.exists() || !dir.isDirectory()) {
            return;
        }
        File[] values = dir.listFiles();

        if (values != null) {
            for (File value : values) {
                if (value.isFile()) {
                    files.add(value);
                }
            }
        }
    }

    // support multiple clients in the same app by using a unique directory path

    private File getStorageDir(String path, @NonNull Configuration config) {
        String apiKey = "" + config.getApiKey().hashCode();
        String endpoint = "" + config.getEndpoint().hashCode();

        File apiDir = new File(path, apiKey);
        apiDir.mkdirs();

        File dir = new File(apiDir, endpoint);
        dir.mkdirs();

        return dir;
    }

}
