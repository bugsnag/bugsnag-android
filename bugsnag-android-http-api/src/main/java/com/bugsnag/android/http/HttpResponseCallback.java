package com.bugsnag.android.http;

import androidx.annotation.NonNull;

public interface HttpResponseCallback<R, S> {
    void onHttpResponse(@NonNull HttpInstrumentedResponse<R, S> response);
}
