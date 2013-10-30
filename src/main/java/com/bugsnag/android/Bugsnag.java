package com.bugsnag.android;

import android.app.Activity;
import android.content.Context;
import android.util.Log;

import com.bugsnag.MetaData;

public class Bugsnag {
    private static Client client;
    private static final String TAG = "Bugsnag";

    public static void register(Context androidContext, String apiKey) {
        register(androidContext, apiKey, true);
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
        runOnClient(new Runnable() {
            @Override
            public void run() {
                client.setContext(context);
            }
        });
    }

    public static void setContext(final Activity context) {
        runOnClient(new Runnable() {
            @Override
            public void run() {
                client.setContext(context);
            }
        });
    }

    public static void setUserId(final String userId) {
        runOnClient(new Runnable() {
            @Override
            public void run() {
                client.setUserId(userId);
            }
        });
    }

    public static void setUser(final String id, final String email, final String name) {
        runOnClient(new Runnable() {
            @Override
            public void run() {
                client.setUser(id, email, name);
            }
        });
    }

    public static void setReleaseStage(final String releaseStage) {
        runOnClient(new Runnable() {
            @Override
            public void run() {
                client.setReleaseStage(releaseStage);
            }
        });
    }

    public static void setNotifyReleaseStages(final String... notifyReleaseStages) {
        runOnClient(new Runnable() {
            @Override
            public void run() {
                client.setNotifyReleaseStages(notifyReleaseStages);
            }
        });
    }

    public static void setAutoNotify(final boolean autoNotify) {
        runOnClient(new Runnable() {
            @Override
            public void run() {
                client.setAutoNotify(autoNotify);
            }
        });
    }

    public static void setUseSSL(final boolean useSSL) {
        runOnClient(new Runnable() {
            @Override
            public void run() {
                client.setUseSSL(useSSL);
            }
        });
    }

    public static void setEndpoint(final String endpoint) {
        runOnClient(new Runnable() {
            @Override
            public void run() {
                client.setEndpoint(endpoint);
            }
        });
    }

    public static void setIgnoreClasses(final String... ignoreClasses) {
        runOnClient(new Runnable() {
            @Override
            public void run() {
                client.setIgnoreClasses(ignoreClasses);
            }
        });
    }

    public static void setProjectPackages(final String... projectPackages) {
        runOnClient(new Runnable() {
            @Override
            public void run() {
                client.setProjectPackages(projectPackages);
            }
        });
    }

    public static void setFilters(final String... filters) {
        runOnClient(new Runnable() {
            @Override
            public void run() {
                client.setFilters(filters);
            }
        });
    }

    public static void addToTab(final String tab, final String key, final Object value) {
        runOnClient(new Runnable() {
            @Override
            public void run() {
                client.addToTab(tab, key, value);
            }
        });
    }

    public static void notify(Throwable e) {
        notify(e, null, null);
    }

    public static void notify(Throwable e, String severity) {
        notify(e, severity, null);
    }

    public static void notify(Throwable e, MetaData overrides) {
        notify(e, null, overrides);
    }

    public static void notify(final Throwable e, final String severity, final MetaData overrides) {
        runOnClient(new Runnable() {
            @Override
            public void run() {
                client.notify(e, severity, overrides);
            }
        });
    }

    public static void onActivityResume(Activity activity) {
        ActivityStack.add(activity);
        ActivityStack.setTopActivity(activity);
    }

    public static void onActivityPause(Activity activity) {
        ActivityStack.clearTopActivity();
    }

    private static void runOnClient(Runnable delegate) {
        if(client != null) {
            try {
                delegate.run();
            } catch(Exception ex) {
                Log.e(TAG, "Error in bugsnag.", ex);
            }
        } else {
            Log.e(TAG, "You must call Bugsnag.register before any other Bugsnag methods.");
        }
    }
}
