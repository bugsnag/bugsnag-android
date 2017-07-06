package com.bugsnag.android;

import java.io.File;

class FileUtils {

    static void clearFilesInDir(File storageDir) {
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
