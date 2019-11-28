package com.bugsnag.android;

import androidx.annotation.NonNull;

public interface OnSession {
    boolean run(@NonNull SessionPayload payload);
}
