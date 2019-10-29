package com.bugsnag.android;

import androidx.annotation.NonNull;

interface OnSession {
    boolean run(@NonNull SessionPayload payload);
}
