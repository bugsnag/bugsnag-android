package com.bugsnag.android;

import java.util.Collection;

public interface BeforeNotify {
    public boolean run(Error error);
}
