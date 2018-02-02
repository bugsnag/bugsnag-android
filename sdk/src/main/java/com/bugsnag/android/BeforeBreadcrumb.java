package com.bugsnag.android;

import android.support.annotation.NonNull;

public interface BeforeBreadcrumb {

    boolean send(@NonNull Breadcrumb breadcrumb);

}
