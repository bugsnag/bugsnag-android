package com.bugsnag.android;

import java.io.Closeable;
import java.io.IOException;

class FileUtils {
    static void close(Closeable stream) {
        if (stream != null) {
            try {
                stream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
