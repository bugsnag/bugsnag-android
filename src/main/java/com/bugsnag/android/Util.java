package com.bugsnag.android;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import android.content.Context;

public class Util {
    public static String getContextName(Context context) {
        String name = context.getClass().getName();
        return name.substring(name.lastIndexOf('.') + 1);
    }

    public static String readFileAsString(File file) {
        String string = null;
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(file);
            StringBuffer fileContent = new StringBuffer();

            byte[] buffer = new byte[1024];
            int length;
            while((length = fis.read(buffer)) != -1) {
                fileContent.append(new String(buffer));
            }

            string = fileContent.toString();
        } catch(IOException e) {
            e.printStackTrace();
        } finally {
            if(fis != null) {
                try { fis.close(); } catch(IOException e) {}
            }
        }

        return string;
    }

}