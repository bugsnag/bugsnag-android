package com.bugsnag.android;

import java.util.Collections;

public class TestOnSendCallback implements OnSendCallback {
    @Override
    public boolean onSend(Event event) {
        event.addMetadata("mazerunner", Collections.singletonMap("onSendCallback", "true"));
        return true;
    }

    public void register(Configuration config) {
        config.addOnSend(this);
    }
}