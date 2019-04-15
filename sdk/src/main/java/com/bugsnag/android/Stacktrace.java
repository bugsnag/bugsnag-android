package com.bugsnag.android;

import android.support.annotation.NonNull;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Serialize an exception stacktrace and mark frames as "in-project"
 * where appropriate.
 */
class Stacktrace implements JsonStream.Streamable {

    private static final int STACKTRACE_TRIM_LENGTH = 200;

    private final List<String> projectPackages;
    final StackTraceElement[] stacktrace;

    Stacktrace(StackTraceElement[] stacktrace, String[] projectPackages) {
        this.stacktrace = stacktrace;
        this.projectPackages = sanitiseProjectPackages(projectPackages);
    }

    @Override
    public void toStream(@NonNull JsonStream writer) throws IOException {
        writer.beginArray();

        for (int k = 0; k < stacktrace.length && k < STACKTRACE_TRIM_LENGTH; k++) {
            StackTraceElement el = stacktrace[k];
            try {
                writer.beginObject();
                if (el.getClassName().length() > 0) {
                    writer.name("method").value(el.getClassName() + "." + el.getMethodName());
                } else {
                    writer.name("method").value(el.getMethodName());
                }
                writer.name("file").value(el.getFileName() == null ? "Unknown" : el.getFileName());
                writer.name("lineNumber").value(el.getLineNumber());

                if (inProject(el.getClassName(), projectPackages)) {
                    writer.name("inProject").value(true);
                }

                writer.endObject();
            } catch (Exception lineEx) {
                Logger.warn("Failed to serialize stacktrace", lineEx);
            }
        }

        writer.endArray();
    }

    private static List<String> sanitiseProjectPackages(String[] projectPackages) {
        if (projectPackages != null) {
            return Arrays.asList(projectPackages);
        } else {
            return Collections.emptyList();
        }
    }

    static boolean inProject(String className, String[] projectPackages) {
        return inProject(className, sanitiseProjectPackages(projectPackages));
    }

    private static boolean inProject(String className, List<String> projectPackages) {
        for (String packageName : projectPackages) {
            if (packageName != null && className.startsWith(packageName)) {
                return true;
            }
        }
        return false;
    }
}
