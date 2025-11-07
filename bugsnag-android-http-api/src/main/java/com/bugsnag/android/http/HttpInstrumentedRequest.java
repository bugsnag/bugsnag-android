package com.bugsnag.android.http;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public interface HttpInstrumentedRequest<R> {
    @NonNull
    R getRequest();

    @Nullable
    String getReportedUrl();

    void setReportedUrl(@Nullable String reportedUrl);

    @Nullable
    String getReportedRequestBody();

    void setReportedRequestBody(@Nullable String requestBody);
}
