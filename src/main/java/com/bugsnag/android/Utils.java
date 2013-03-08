package com.bugsnag.android;

import java.io.File;
import java.io.RandomAccessFile;
import java.io.FileWriter;
import java.io.IOException;

import android.os.SystemClock;

class Utils {
    public static String readFileAsString(File file) throws IOException {
        RandomAccessFile f = new RandomAccessFile(file, "r");
        byte[] bytes = new byte[(int) f.length()];
        f.readFully(bytes);
        f.close();
        return new String(bytes);
    }

    public static void writeStringToFile(String str, String path) throws IOException {
        FileWriter writer = null;
        try {
            writer = new FileWriter(path);
            writer.write(str);
            writer.flush();
        } finally {
            if(writer != null) {
                writer.close();
            }
        }
    }

    public static long secondsSinceBoot() {
        return (long)(SystemClock.elapsedRealtime()/1000);
    }
}