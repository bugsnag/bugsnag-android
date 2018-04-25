package com.bugsnag.android;

import java.io.File;

final class FileUtils {

    private FileUtils() {
    }

    static void clearFiles(FileStore fileStore) {
        File storageDir = fileStore.storageDir;
        clearFilesInDir(storageDir);
        clearFilesInDir(new File(fileStore.oldDirectory));
    }

    private static void clearFilesInDir(File storageDir) {
        if (!storageDir.isDirectory()) {
            throw new IllegalArgumentException();
        }
        for (File file : storageDir.listFiles()) {
            if (!file.isDirectory()) {
                file.delete();
            }
        }
    }

}
