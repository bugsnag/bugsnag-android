package com.bugsnag.android;

import android.content.Context;
import android.content.pm.ApplicationInfo;;

/**
 * This class contains information about the current app which should
 * not change over time. App information in this class should be cached
 * for fast subsequent lookups.
 */
class App implements JsonStream.Streamable {
    private Configuration config;
    private Context appContext;
    private String packageName;

    public App(Configuration config, Context appContext) {
        this.config = config;
        this.appContext = appContext;

        packageName = appContext.getPackageName();
    }

    public void toStream(JsonStream writer) {
        writer.object()
            .name("id").value(packageName)
            .name("name").value(appName.get())
            .name("packageName").value(packageName)
            .name("versionName").value(versionName.get())
            .name("versionCode").value(versionCode.get());

        if(config.appVersion != null) {
            writer.name("version").value(config.appVersion);
        } else {
            writer.name("version").value(versionName.get());
        }

        if(config.releaseStage != null) {
            writer.name("releaseStage").value(config.releaseStage);
        } else {
            writer.name("releaseStage").value(releaseStage.get());
        }

        writer.endObject();
    }

    private CachedValue<String> appName = new CachedValue<String>() {
        @Override
        public String calc() throws Exception {
            return appContext.getPackageManager().getApplicationInfo(packageName, 0).name;
        }
    };

    private CachedValue<String> versionName = new CachedValue<String>() {
        @Override
        public String calc() throws Exception {
            return appContext.getPackageManager().getPackageInfo(packageName, 0).versionName;
        }
    };

    private CachedValue<Integer> versionCode = new CachedValue<Integer>() {
        @Override
        public Integer calc() throws Exception {
            return appContext.getPackageManager().getPackageInfo(packageName, 0).versionCode;
        }
    };

    private CachedValue<String> releaseStage = new CachedValue<String>() {
        @Override
        public String calc() throws Exception {
            int appFlags = appContext.getPackageManager().getApplicationInfo(packageName, 0).flags;
            if((appFlags & ApplicationInfo.FLAG_DEBUGGABLE) != 0) {
                return "development";
            }

            return "production";
        }
    };
}
