package com.bugsnag.android;

import androidx.annotation.NonNull;

public interface OnSessionCallback {
    boolean onSession(@NonNull Session session);
}
