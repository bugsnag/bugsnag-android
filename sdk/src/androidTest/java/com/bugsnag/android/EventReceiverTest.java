package com.bugsnag.android;

import android.support.test.filters.SmallTest;

import org.junit.Test;

import static junit.framework.Assert.assertEquals;

@SmallTest
public class EventReceiverTest {

    @Test
    public void checkActionName() throws Exception {
        assertEquals("CONNECTION_STATE_CHANGE",
            EventReceiver.shortenActionNameIfNeeded("android.net.wifi.p2p.CONNECTION_STATE_CHANGE"));

        assertEquals("CONNECTION_STATE_CHANGE",
            EventReceiver.shortenActionNameIfNeeded("CONNECTION_STATE_CHANGE"));

        assertEquals("NOT_ANDROID.net.wifi.p2p.CONNECTION_STATE_CHANGE",
            EventReceiver.shortenActionNameIfNeeded("NOT_ANDROID.net.wifi.p2p.CONNECTION_STATE_CHANGE"));
    }

}
