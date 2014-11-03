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
class Device implements JsonStreamer.Streamable {
    private Configuration config;
    private Context appContext;
    private String packageName;

    public Device(Configuration config, Context appContext) {
        this.config = config;
        this.appContext = appContext;

        packageName = appContext.getPackageName();
    }

    public void toStream(JsonStreamer writer) {
        writer.beginObject()
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

    private CachedValue<Float> screenDensity = new CachedValue<Float>() {
        @Override
        public Float calc() {
            return appContext.getResources().getDisplayMetrics().density;
        }
    };

    private CachedValue<String> screenResolution = new CachedValue<String>() {
        @Override
        public String calc() {
            DisplayMetrics metrics = appContext.getResources().getDisplayMetrics();
            return String.format("%dx%d", Math.max(metrics.widthPixels, metrics.heightPixels), Math.min(metrics.widthPixels, metrics.heightPixels));
        }
    };

    private CachedValue<Long> totalMemory = new CachedValue<Long>() {
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

    private CachedValue<Boolean> rooted = new CachedValue<Boolean>() {
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

    private CachedValue<String> locale = new CachedValue<String>() {
        @Override
        public String calc() {
            return Locale.getDefault().toString();
        }
    };
}
