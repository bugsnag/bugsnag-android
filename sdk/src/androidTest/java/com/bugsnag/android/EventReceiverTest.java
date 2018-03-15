package com.bugsnag.android;

import static com.bugsnag.android.EventReceiver.shortenActionNameIfNeeded;
import static junit.framework.Assert.assertEquals;

import android.support.test.filters.SmallTest;

import org.junit.Test;

@SmallTest
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
