package com.bugsnag.android;

import androidx.annotation.Nullable;

import java.util.UUID;

class TestHooks {
    static boolean getUnhandledOverridden(Event event) {
        return event.getImpl().getUnhandledOverridden();
    }

    @Nullable
    static UUID getCorrelatedTraceId(Event event) {
        TraceCorrelation traceCorrelation = event.getImpl().getTraceCorrelation();
        return traceCorrelation != null ? traceCorrelation.getTraceId() : null;
    }

    @Nullable
    static Long getCorrelatedSpanId(Event event) {
        TraceCorrelation traceCorrelation = event.getImpl().getTraceCorrelation();
        return traceCorrelation != null ? traceCorrelation.getSpanId() : null;
    }

    static MetadataState generateMetadataState() {
        return new MetadataState();
    }

    static FeatureFlagState generateFeatureFlagsState() {
        return new FeatureFlagState();
    }
}
