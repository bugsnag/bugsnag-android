package com.bugsnag.android;

import static org.junit.Assert.assertEquals;

import androidx.annotation.NonNull;
import androidx.test.filters.SmallTest;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Ensures that BeforeNotify is only called once,
 * and that the callbacks are called in insertion order.
 */
@SmallTest
public class UniqueBeforeNotifyTest {

    private BeforeNotify firstCb = new BeforeNotify() {
        @Override
        public boolean run(@NonNull Error error) {
            return handleCallback();
        }
    };

    private BeforeNotify secondCb = new BeforeNotify() {
        @Override
        public boolean run(@NonNull Error error) {
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

    /**
     * Tears down the test.
     * lol why is this comment required
     */
    @After
    public void tearDown() {
        callbackCount = 0;
        client.close();
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
            public boolean run(@NonNull Error error) {
                assertEquals(0, callbackCount);
                callbackCount++;
                return false;
            }
        });
        client.beforeNotify(new BeforeNotify() {
            @Override
            public boolean run(@NonNull Error error) {
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
