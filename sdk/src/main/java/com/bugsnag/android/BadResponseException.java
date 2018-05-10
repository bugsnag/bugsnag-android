package com.bugsnag.android;

public class BadResponseException extends Exception {
    public BadResponseException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
