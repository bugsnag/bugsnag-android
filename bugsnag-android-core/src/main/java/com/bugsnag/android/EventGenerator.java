package com.bugsnag.android;

import androidx.annotation.NonNull;

import java.lang.Thread;

class EventGenerator {

    static class Builder {
        private final ImmutableConfig config;
        private final Throwable exception;
        private final SessionTracker sessionTracker;
        private final ThreadState threadState;
        private Severity severity = Severity.WARNING;
        private MetaData metaData = new MetaData();
        private MetaData globalMetaData;
        private String attributeValue;

        @HandledState.SeverityReason
        private String severityReasonType;

        Builder(@NonNull ImmutableConfig config,
                @NonNull Throwable exception,
                SessionTracker sessionTracker,
                @NonNull Thread thread,
                boolean unhandled,
                MetaData globalMetaData) {
            Throwable exc = unhandled ? exception : null;
            this.threadState = new ThreadState(config, thread, Thread.getAllStackTraces(), exc);
            this.config = config;
            this.exception = exception;
            this.severityReasonType = HandledState.REASON_USER_SPECIFIED; // default
            this.sessionTracker = sessionTracker;
            this.globalMetaData = globalMetaData;
        }

        Builder(@NonNull ImmutableConfig config, @NonNull String name,
                @NonNull String message, @NonNull StackTraceElement[] frames,
                SessionTracker sessionTracker, Thread thread, MetaData globalMetaData) {
            this(config, new BugsnagException(name, message, frames), sessionTracker,
                    thread, false, globalMetaData);
        }

        Builder severityReasonType(@HandledState.SeverityReason String severityReasonType) {
            this.severityReasonType = severityReasonType;
            return this;
        }

        Builder attributeValue(String value) {
            this.attributeValue = value;
            return this;
        }

        Builder severity(Severity severity) {
            this.severity = severity;
            return this;
        }

        Builder metaData(MetaData metaData) {
            this.metaData = metaData;
            return this;
        }

        Event build() {
            HandledState handledState =
                    HandledState.newInstance(severityReasonType, severity, attributeValue);
            Session session = getSession(handledState);
            MetaData metaData = MetaData.Companion.merge(globalMetaData, this.metaData);
            return new Event(config, exception, handledState,
                    severity, session, threadState, metaData);
        }

        private Session getSession(HandledState handledState) {
            if (sessionTracker == null) {
                return null;
            }
            Session currentSession = sessionTracker.getCurrentSession();
            Session reportedSession = null;

            if (currentSession == null) {
                return null;
            }
            if (config.getAutoTrackSessions() || !currentSession.isAutoCaptured()) {
                if (handledState.isUnhandled()) {
                    reportedSession = sessionTracker.incrementUnhandledAndCopy();
                } else {
                    reportedSession = sessionTracker.incrementHandledAndCopy();
                }
            }
            return reportedSession;
        }
    }

}
