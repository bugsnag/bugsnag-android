package com.bugsnag.android;

import java.io.Closeable;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;

class IOUtils {
    static void close(Closeable stream) {
        if (stream != null) {
            try {
                stream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    static int copy(File file, Writer output) throws IOException {
        return copy(new FileReader(file), output);
    }

    static int copy(Reader input, Writer output) throws IOException {
        char[] buffer = new char[1024 * 4];
        int count = 0;
        int n = 0;
        while (-1 != (n = input.read(buffer))) {
            output.write(buffer, 0, n);
            count += n;
        }
        return count;
    }
}
