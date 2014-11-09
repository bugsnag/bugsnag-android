package com.bugsnag.android;

import java.io.File;
import java.util.Locale;

import android.content.Context;
import android.util.DisplayMetrics;

/**
 * This class contains information about the current device which should
 * not change over time. Device information in this class should be cached
 * for fast subsequent lookups.
 */
class DeviceData implements JsonStream.Streamable {
    private Configuration config;
    private Context appContext;
    private String packageName;

    DeviceData(Configuration config, Context appContext) {
        this.config = config;
        this.appContext = appContext;

        packageName = appContext.getPackageName();
    }

    public void toStream(JsonStream writer) {
        writer.object()
            .name("manufacturer").value(android.os.Build.MANUFACTURER)
            .name("model").value(android.os.Build.MODEL)
            .name("screenDensity").value(screenDensity.get())
            .name("screenResolution").value(screenResolution.get())
            .name("totalMemory").value(totalMemory.get())
            .name("osName").value("android")
            .name("osBuild").value(android.os.Build.DISPLAY)
            .name("apiLevel").value(android.os.Build.VERSION.SDK_INT)
            .name("jailbroken").value(rooted.get())
            .name("locale").value(locale.get())
            .name("osVersion").value(android.os.Build.VERSION.RELEASE)
            .name("id").value("TODO")
        .endObject();
    }

    private CachedValue<Float> screenDensity = new CachedValue<Float>("DeviceData.screenDensity") {
        @Override
        public Float calc() {
            return appContext.getResources().getDisplayMetrics().density;
        }
    };

    private CachedValue<String> screenResolution = new CachedValue<String>("DeviceData.screenResolution") {
        @Override
        public String calc() {
            DisplayMetrics metrics = appContext.getResources().getDisplayMetrics();
            return String.format("%dx%d", Math.max(metrics.widthPixels, metrics.heightPixels), Math.min(metrics.widthPixels, metrics.heightPixels));
        }
    };

    private CachedValue<Long> totalMemory = new CachedValue<Long>("DeviceData.totalMemory") {
        @Override
        public Long calc() {
            Long totalMemory = null;
            if(Runtime.getRuntime().maxMemory() != Long.MAX_VALUE) {
                totalMemory = Runtime.getRuntime().maxMemory();
            } else {
                totalMemory = Runtime.getRuntime().totalMemory();
            }
            return totalMemory;
        }
    };

    private CachedValue<Boolean> rooted = new CachedValue<Boolean>("DeviceData.rooted") {
        @Override
        public Boolean calc() {
            boolean hasTestKeys = android.os.Build.TAGS != null && android.os.Build.TAGS.contains("test-keys");
            boolean hasSuperUserApk = false;
            try {
                File file = new File("/system/app/Superuser.apk");
                hasSuperUserApk = file.exists();
            } catch (Exception e) { }

            return hasTestKeys || hasSuperUserApk;
        }
    };

    private CachedValue<String> locale = new CachedValue<String>("DeviceData.locale") {
        @Override
        public String calc() {
            return Locale.getDefault().toString();
        }
    };
}
