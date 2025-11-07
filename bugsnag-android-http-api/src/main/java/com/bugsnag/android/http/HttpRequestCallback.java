package com.bugsnag.android.http;

import androidx.annotation.NonNull;

public interface HttpRequestCallback<R> {
    void onHttpRequest(@NonNull HttpInstrumentedRequest<R> req);
}
