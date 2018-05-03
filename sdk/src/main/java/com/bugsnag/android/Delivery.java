package com.bugsnag.android;

interface Delivery {

    void deliver(SessionTrackingPayload payload,
                 Configuration config) throws DeliveryFailureException;

    void deliver(Report report,
                 Configuration config) throws DeliveryFailureException;
}
