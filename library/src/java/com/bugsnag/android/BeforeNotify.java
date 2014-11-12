package com.bugsnag.android;

public interface BeforeNotify {
    abstract boolean run (Error error);
}
