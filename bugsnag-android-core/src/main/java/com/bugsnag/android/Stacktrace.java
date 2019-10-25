package com.bugsnag.android;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Serialize an exception stacktrace and mark frames as "in-project"
 * where appropriate.
 */
class Stacktrace implements JsonStream.Streamable {

    private static final int STACKTRACE_TRIM_LENGTH = 200;

    private final List<Map<String, Object>> trace;

    Stacktrace(StackTraceElement[] stacktrace, Collection<String> projectPackages) {
        this.trace = serializeStacktrace(stacktrace, projectPackages);
    }

    Stacktrace(List<Map<String, Object>> frames) {
        if (frames.size() >= STACKTRACE_TRIM_LENGTH) {
            this.trace = frames.subList(0, STACKTRACE_TRIM_LENGTH);
        } else {
            this.trace = frames;
        }
    }

    @Override
    public void toStream(@NonNull JsonStream writer) throws IOException {
        writer.beginArray();
        for (Map<String, Object> element : trace) {
            writer.value(element);
        }
        writer.endArray();
    }

    private List<Map<String, Object>> serializeStacktrace(StackTraceElement[] trace,
                                                          Collection<String> projectPackages) {
        List<Map<String, Object>> list = new ArrayList<>();

        for (int k = 0; k < trace.length && k < STACKTRACE_TRIM_LENGTH; k++) {
            StackTraceElement el = trace[k];
            Map<String, Object> frame = serializeStackframe(el, projectPackages);

            if (frame != null) {
                list.add(frame);
            }
        }
        return list;
    }

    @Nullable
    private Map<String, Object> serializeStackframe(StackTraceElement el,
                                                    Collection<String> projectPackages) {
        Map<String, Object> map = new HashMap<>();
        try {
            String methodName;
            if (el.getClassName().length() > 0) {
                methodName = el.getClassName() + "." + el.getMethodName();
            } else {
                methodName =  el.getMethodName();
            }
            map.put("method", methodName);

            String filename = el.getFileName() == null ? "Unknown" : el.getFileName();
            map.put("file", filename);
            map.put("lineNumber", el.getLineNumber());

            if (inProject(el.getClassName(), projectPackages)) {
                map.put("inProject", true);
            }
            return map;
        } catch (Exception lineEx) {
            return null;
        }
    }

    private static boolean inProject(String className, Collection<String> projectPackages) {
        for (String packageName : projectPackages) {
            if (packageName != null && className.startsWith(packageName)) {
                return true;
            }
        }
        return false;
    }
}
