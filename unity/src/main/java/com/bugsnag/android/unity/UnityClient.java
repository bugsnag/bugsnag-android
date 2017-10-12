package com.bugsnag.android.unity;

import android.content.Context;

import com.bugsnag.android.Bugsnag;
import com.bugsnag.android.BugsnagException;
import com.bugsnag.android.Severity;

import java.util.HashMap;
import java.util.Map;


public class UnityClient {

    public static void init(Context androidContext, String apiKey) {
        Bugsnag.init(androidContext, apiKey);
    }

    public static void notify(String name, String message,
                              String context, StackTraceElement[] stacktrace,
                              Severity severity, String logLevel,
                              String severityReason) {
        Throwable t = new BugsnagException(name, message, stacktrace);

        Map<String, Object> data = new HashMap<>();
        data.put("severity", severity.getName());
        data.put("severityReason", severityReason);
        data.put("logLevel", logLevel);

        Bugsnag.getClient().internalClientNotify(t, data, false, new UnityCallback(context, logLevel));
    }

}
