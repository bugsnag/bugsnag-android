package com.bugsnag.android;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import org.json.JSONObject;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageInfo;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.SystemClock;

import com.bugsnag.Error;
import com.bugsnag.MetaData;
import com.bugsnag.Metrics;
import com.bugsnag.Notification;
import com.bugsnag.http.HttpClient;
import com.bugsnag.http.NetworkException;
import com.bugsnag.http.BadResponseException;
import com.bugsnag.utils.JSONUtils;

public class Bugsnag {
    private static Client client;
    private static final String TAG = "Bugsnag";

    static long startTime = SystemClock.elapsedRealtime();

    public static void register(Context androidContext, String apiKey) {
        register(androidContext, apiKey, false);
    }

    public static void register(Context androidContext, String apiKey, boolean enableMetrics) {
        // Create the bugsnag client
        try {
            client = new Client(androidContext, apiKey, enableMetrics);
        } catch(Exception ex) {
            Log.e(TAG, "Unable to register with bugsnag. ", ex);
        }
    }

    public static void setContext(final String context) {
        performOnClient(new AnonymousDelegate(){
            public void perform() {
                client.setContext(context);
            }
        });
    }

    public static void setContext(final Activity context) {
        performOnClient(new AnonymousDelegate(){
            public void perform() {
                client.setContext(context);
            }
        });
    }

    public static void setUserId(final String userId) {
        performOnClient(new AnonymousDelegate(){
            public void perform() {
                client.setUserId(userId);
            }
        });
    }

    public static void setReleaseStage(final String releaseStage) {
        performOnClient(new AnonymousDelegate(){
            public void perform() {
                client.setReleaseStage(releaseStage);
            }
        });
    }

    public static void setNotifyReleaseStages(final String... notifyReleaseStages) {
        performOnClient(new AnonymousDelegate(){
            public void perform() {
                client.setNotifyReleaseStages(notifyReleaseStages);
            }
        });
    }

    public static void setAutoNotify(final boolean autoNotify) {
        performOnClient(new AnonymousDelegate(){
            public void perform() {
                client.setAutoNotify(autoNotify);
            }
        });
    }

    public static void setUseSSL(final boolean useSSL) {
        performOnClient(new AnonymousDelegate(){
            public void perform() {
                client.setUseSSL(useSSL);
            }
        });
    }

    public static void setEndpoint(final String endpoint) {
        performOnClient(new AnonymousDelegate(){
            public void perform() {
                client.setEndpoint(endpoint);
            }
        });
    }

    public static void setIgnoreClasses(final String... ignoreClasses) {
        performOnClient(new AnonymousDelegate(){
            public void perform() {
                client.setIgnoreClasses(ignoreClasses);
            }
        });
    }

    public static void addToTab(final String tab, final String key, final Object value) {
        performOnClient(new AnonymousDelegate(){
            public void perform() {
                client.addToTab(tab, key, value);
            }
        });
    }

    public static void notify(Throwable e) {
        notify(e, null);
    }

    public static void notify(final Throwable e, final MetaData overrides) {
        performOnClient(new AnonymousDelegate(){
            public void perform() {
                client.notify(e, overrides);
            }
        });
    }

    interface AnonymousDelegate {
        void perform();
    }

    private static void performOnClient(AnonymousDelegate delegate) {
        if(client != null) {
            try {
                delegate.perform();
            } catch(Exception ex) {
                Log.e(TAG, "Error in bugsnag.", ex);
            }
        } else {
            Log.e(TAG, "Method called on Bugsnag before register.");
        }
    }
}