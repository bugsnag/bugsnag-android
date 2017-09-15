package com.bugsnag.android;

import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;

/**
 * Ensures that BeforeNotify is only called once,
 * and that the callbacks are called in insertion order.
 */
@RunWith(AndroidJUnit4.class)
@SmallTest
public class UniqueBeforeNotifyTest {

    private BeforeNotify firstCb = new BeforeNotify() {
        @Override
        public boolean run(Error error) {
            return handleCallback();
        }
    };

    private BeforeNotify secondCb = new BeforeNotify() {
        @Override
        public boolean run(Error error) {
            return handleCallback();
        }
    };

    private int callbackCount;
    private Client client;

    @Before
    public void setUp() throws Exception {
        callbackCount = 0;
        client = BugsnagTestUtils.generateClient();
    }

    @After
    public void tearDown() throws Exception {
        callbackCount = 0;
    }

    @Test
    public void checkBeforeNotify() {
        client.beforeNotify(firstCb);
        client.notify(new Throwable());
        assertEquals(1, callbackCount);
    }

    @Test
    public void testDuplicateCallback() {
        client.beforeNotify(firstCb);
        client.beforeNotify(firstCb);
        client.beforeNotify(secondCb);
        client.beforeNotify(secondCb);
        client.notify(new Throwable());
        assertEquals(2, callbackCount);
    }

    @Test
    public void testCallbackOrder() {
        client.beforeNotify(new BeforeNotify() {
            @Override
            public boolean run(Error error) {
                assertEquals(0, callbackCount);
                callbackCount++;
                return false;
            }
        });
        client.beforeNotify(new BeforeNotify() {
            @Override
            public boolean run(Error error) {
                assertEquals(1, callbackCount);
                return false;
            }
        });
        client.notify(new Throwable());
    }

    private boolean handleCallback() {
        callbackCount++;
        return true;
    }

}
