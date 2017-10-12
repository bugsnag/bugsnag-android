package com.bugsnag.android.unity;

import com.bugsnag.android.Callback;
import com.bugsnag.android.MetaData;
import com.bugsnag.android.Report;


class UnityCallback implements Callback {

    static final String NOTIFIER_NAME = "Bugsnag Unity (Android)";
    static final String NOTIFIER_VERSION = "3.5.1";
    static final String NOTIFIER_URL = "https://github.com/bugsnag/bugsnag-unity";

    private final String context;
    private final String logLevel;

    UnityCallback(String context, String logLevel) {
        this.context = context;
        this.logLevel = logLevel;
    }

    @Override
    public void beforeNotify(Report report) {
        report.setNotifierName(NOTIFIER_NAME);
        report.setNotifierURL(NOTIFIER_URL);
        report.setNotifierVersion(NOTIFIER_VERSION);

        com.bugsnag.android.Error error = report.getError();
        MetaData metadata = error.getMetaData();
        metadata.addToTab("Unity", "unityException", "true");

        if (logLevel != null && logLevel.length() > 0) {
            metadata.addToTab("Unity", "unityLogLevel", logLevel);
        }
        if (context != null && context.length() > 0) {
            error.setContext(context);
        }
    }
}
