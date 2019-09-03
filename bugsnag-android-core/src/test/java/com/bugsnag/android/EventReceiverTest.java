package com.bugsnag.android;

import static com.bugsnag.android.EventReceiver.shortenActionNameIfNeeded;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class EventReceiverTest {

    @Test
    public void checkActionName() throws Exception {
        assertEquals("CONNECTION_STATE_CHANGE",
            shortenActionNameIfNeeded("android.net.wifi.p2p.CONNECTION_STATE_CHANGE"));

        assertEquals("CONNECTION_STATE_CHANGE",
            shortenActionNameIfNeeded("CONNECTION_STATE_CHANGE"));

        assertEquals("NOT_ANDROID.net.wifi.p2p.CONNECTION_STATE_CHANGE",
            shortenActionNameIfNeeded("NOT_ANDROID.net.wifi.p2p.CONNECTION_STATE_CHANGE"));
    }

}
