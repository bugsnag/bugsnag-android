package com.bugsnag.android;

import android.app.Activity;
import android.content.Context;
import android.util.Log;

import com.bugsnag.MetaData;

public class Bugsnag {
    private static Client client;

    public static void register(Context androidContext, String apiKey) {
        client = new Client(androidContext, apiKey);
    }

    public static void notify(Throwable e) {
        notify(e, null);
    }

    public static void notify(Throwable e, MetaData metaData) {
        if(client == null) {
            Log.e("Bugsnag", "You must call register with an apiKey before we can notify of exceptions!");
            return;
        }

        client.notify(e, metaData);
    }

    public static void setContext(String context) {
        client.setContext(context);
    }

    public static void setContext(Activity context) {
        client.setContext(context);
    }

    public static void setUserId(String userId) {
        client.setUserId(userId);
    }

    public static void setReleaseStage(String releaseStage) {
        client.setReleaseStage(releaseStage);
    }

    public static void setNotifyReleaseStages(String... notifyReleaseStages) {
        client.setNotifyReleaseStages(notifyReleaseStages);
    }

    public static void setAutoNotify(boolean autoNotify) {
        client.setAutoNotify(autoNotify);
    }

    public static void setUseSSL(boolean useSSL) {
        client.setUseSSL(useSSL);
    }

    public static void setEndpoint(String endpoint) {
        client.setEndpoint(endpoint);
    }

    public static void addActivity(Activity activity) {
        client.addActivity(activity);
    }
}