package com.bugsnag.android;


import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.UUID;

class DeviceIdSource {
    private final Context context;
    private final SharedPreferences preferences;

    DeviceIdSource(@NonNull Context context) {
        this.context = context;
        preferences = context.getSharedPreferences(Client.SHARED_PREF_KEY, Context.MODE_PRIVATE);
    }

    @NonNull
    public String getId() {
        String currentId = preferences.getString(Client.DEVICE_TOKEN_KEY, null);
        if (currentId == null) {
            if (isLegacyInstall()) currentId = getLegacyId();
            if (currentId == null) currentId = UUID.randomUUID().toString();

            final SharedPreferences.Editor editor = preferences.edit().putString(Client.DEVICE_TOKEN_KEY, currentId);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) editor.apply();
            else editor.commit();
        }
        return currentId;
    }

    private boolean isLegacyInstall() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.GINGERBREAD) return false;
        try {
            final PackageInfo info = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            return info.firstInstallTime != info.lastUpdateTime;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            return false;
        }
    }

    @SuppressLint("HardwareIds")
    @Nullable
    private String getLegacyId() {
        ContentResolver cr = context.getContentResolver();
        return Settings.Secure.getString(cr, Settings.Secure.ANDROID_ID);
    }
}

