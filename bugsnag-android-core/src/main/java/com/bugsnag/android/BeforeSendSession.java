package com.bugsnag.android;

interface BeforeSendSession {
    void beforeSendSession(SessionPayload payload);
}
