package com.bugsnag.android;

import android.support.annotation.NonNull;

import java.util.Map;

public interface BeforeBreadcrumb {

    boolean send(@NonNull String name,
                 @NonNull BreadcrumbType breadcrumbType,
                 @NonNull Map<String, String> metadata);

}
