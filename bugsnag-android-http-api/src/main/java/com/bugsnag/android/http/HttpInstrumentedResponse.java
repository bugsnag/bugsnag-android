package com.bugsnag.android.http;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public interface HttpInstrumentedResponse<R, S> {
    @NonNull
    R getRequest();

    @Nullable
    S getResponse();

    @Nullable
    String getReportedResponseBody();

    void setReportedResponseBody(@Nullable String responseBody);
}
