package com.bugsnag.android;

interface BeforeSendSession {
    void beforeSendSession(SessionTrackingPayload payload);
}
