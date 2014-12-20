package com.bugsnag.android;

import android.content.Context;
import android.content.pm.ApplicationInfo;;
import android.content.pm.PackageManager;

/**
 * Information about the running Android app which doesn't change over time,
 * including app name, version and release stage.
 *
 * App information in this class is cached during construction for faster
 * subsequent lookups and to reduce GC overhead.
 */
class AppData implements JsonStream.Streamable {
    private Configuration config;
    private Context appContext;

    private String packageName;
    private String appName;
    private Integer versionCode;
    private String versionName;
    private String guessedReleaseStage;

    AppData(Context appContext, Configuration config) {
        this.config = config;
        this.appContext = appContext;

        packageName = getPackageName();
        appName = getAppName();
        versionCode = getVersionCode();
        versionName = getVersionName();
        guessedReleaseStage = guessReleaseStage();
    }

    public void toStream(JsonStream writer) {
        writer.beginObject();
            writer.name("id").value(packageName);
            writer.name("name").value(appName);
            writer.name("packageName").value(packageName);
            writer.name("versionName").value(versionName);
            writer.name("versionCode").value(versionCode);

            // Prefer user-configured appVersion + releaseStage
            writer.name("version").value(getAppVersion());
            writer.name("releaseStage").value(getReleaseStage());

        writer.endObject();
    }

    public String getReleaseStage() {
        if(config.releaseStage != null) {
            return config.releaseStage;
        } else {
            return guessedReleaseStage;
        }
    }

    public String getAppVersion() {
        if(config.appVersion != null) {
            return config.appVersion;
        } else {
            return versionName;
        }
    }

    /**
     * The package name of the running Android app, eg: com.example.myapp
     */
    private String getPackageName() {
        return appContext.getPackageName();
    }

    /**
     * The name of the running Android app, from android:label in
     * AndroidManifest.xml
     */
    private String getAppName() {
        try {
            PackageManager packageManager = appContext.getPackageManager();
            ApplicationInfo appInfo = packageManager.getApplicationInfo(packageName, 0);

            return (String)packageManager.getApplicationLabel(appInfo);
        } catch (PackageManager.NameNotFoundException e) {
            Logger.warn("Could not get app name");
        }

        return null;
    }

    /**
     * The version code of the running Android app, from android:versionCode
     * in AndroidManifest.xml
     */
    private Integer getVersionCode() {
        try {
            return appContext.getPackageManager().getPackageInfo(packageName, 0).versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            Logger.warn("Could not get versionCode");
        }
        return null;
    }

    /**
     * The version code of the running Android app, from android:versionName
     * in AndroidManifest.xml
     */
    private String getVersionName() {
        try {
            return appContext.getPackageManager().getPackageInfo(packageName, 0).versionName;
        } catch (PackageManager.NameNotFoundException e) {
            Logger.warn("Could not get versionName");
        }
        return null;
    }

    /**
     * Guess the release stage of the running Android app by checking the
     * android:debuggable flag from AndroidManifest.xml
     */
    private String guessReleaseStage() {
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
