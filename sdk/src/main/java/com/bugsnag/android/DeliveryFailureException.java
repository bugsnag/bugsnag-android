package com.bugsnag.android;

public class DeliveryFailureException extends Exception {

    enum Reason {
        CONNECTIVITY,
        BAD_REQUEST
    }

    final Reason reason;

    public DeliveryFailureException(Reason reason, String msg) {
        super(msg);
        this.reason = reason;
    }

    public DeliveryFailureException(Reason reason, String msg, Throwable throwable) {
        super(msg, throwable);
        this.reason = reason;
    }
}
