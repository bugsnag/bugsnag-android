package com.bugsnag.android;

import android.content.Context;
import android.content.pm.ApplicationInfo;;
import android.content.pm.PackageManager;

/**
 * This class contains information about the current app which should
 * not change over time. App information in this class should be cached
 * for fast subsequent lookups.
 */
class AppData implements JsonStream.Streamable {
    private Configuration config;
    private Context appContext;

    private String packageName;
    private String appName;
    private Integer versionCode;
    private String versionName;
    private String releaseStage;

    AppData(Context appContext, Configuration config) {
        this.config = config;
        this.appContext = appContext;

        packageName = getPackageName();
        appName = getAppName();
        versionCode = getVersionCode();
        versionName = getVersionName();
        releaseStage = getReleaseStage();
    }

    public void toStream(JsonStream writer) {
        writer.beginObject()
            .name("id").value(packageName)
            .name("name").value(appName)
            .name("packageName").value(packageName)
            .name("versionName").value(versionName)
            .name("versionCode").value(versionCode);

            // Prefer user-configured appVersion
            if(config.appVersion != null) {
                writer.name("version").value(config.appVersion);
            } else {
                writer.name("version").value(versionName);
            }

            // Prefer user-configured releaseStage
            if(config.releaseStage != null) {
                writer.name("releaseStage").value(config.releaseStage);
            } else {
                writer.name("releaseStage").value(releaseStage);
            }

        writer.endObject();
    }

    public String getPackageName() {
        return appContext.getPackageName();
    }

    public String getAppName() {
        try {
            PackageManager packageManager = appContext.getPackageManager();
            ApplicationInfo appInfo = packageManager.getApplicationInfo(packageName, 0);

            return (String)packageManager.getApplicationLabel(appInfo);
        } catch (PackageManager.NameNotFoundException e) {
            Logger.warn("Could not get app name");
        }

        return null;
    }

    public Integer getVersionCode() {
        try {
            return appContext.getPackageManager().getPackageInfo(packageName, 0).versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            Logger.warn("Could not get versionCode");
        }
        return null;
    }

    public String getVersionName() {
        try {
            return appContext.getPackageManager().getPackageInfo(packageName, 0).versionName;
        } catch (PackageManager.NameNotFoundException e) {
            Logger.warn("Could not get versionName");
        }
        return null;
    }

    public String getReleaseStage() {
        try {
            int appFlags = appContext.getPackageManager().getApplicationInfo(packageName, 0).flags;
            if((appFlags & ApplicationInfo.FLAG_DEBUGGABLE) != 0) {
                return "development";
            }
        } catch (PackageManager.NameNotFoundException e) {
            Logger.warn("Could not get releaseStage");
        }
        return "production";
    }
}
