package com.bugsnag.android;

import static org.junit.Assert.assertEquals;

import androidx.annotation.NonNull;
import androidx.test.filters.SmallTest;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Ensures that OnError is only called once,
 * and that the callbacks are called in insertion order.
 */
@SmallTest
public class UniqueOnErrorTest {

    private final OnError firstCb = new OnError() {
        @Override
        public boolean run(@NonNull Event event) {
            return handleCallback();
        }
    };

    private final OnError secondCb = new OnError() {
        @Override
        public boolean run(@NonNull Event event) {
            return handleCallback();
        }
    };

    private int callbackCount;
    private Client client;

    @Before
    public void setUp() {
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
    public void checkOnError() {
        client.addOnError(firstCb);
        client.notify(new Throwable());
        assertEquals(1, callbackCount);
    }

    @Test
    public void testDuplicateCallback() {
        client.addOnError(firstCb);
        client.addOnError(firstCb);
        client.addOnError(secondCb);
        client.addOnError(secondCb);
        client.notify(new Throwable());
        assertEquals(4, callbackCount);
    }

    @Test
    public void testCallbackOrder() {
        client.addOnError(new OnError() {
            @Override
            public boolean run(@NonNull Event event) {
                assertEquals(0, callbackCount);
                callbackCount++;
                return false;
            }
        });
        client.addOnError(new OnError() {
            @Override
            public boolean run(@NonNull Event event) {
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
