package com.bugsnag.android;

/**
 * Ensures that BeforeNotify is only called once,
 * and that the callbacks are called in insertion order.
 */
public class UniqueBeforeNotifyTest extends BugsnagTestCase {

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

    @Override
    public void setUp() throws Exception {
        super.setUp();
        callbackCount = 0;
        client = new Client(getContext(), "123");
    }

    @Override
    public void tearDown() throws Exception {
        super.tearDown();
        callbackCount = 0;
    }

    public void testBeforeNotify() {
        client.beforeNotify(firstCb);
        client.notify(new Throwable());
        assertEquals(1, callbackCount);
    }

    public void testDuplicateCallback() {
        client.beforeNotify(firstCb);
        client.beforeNotify(firstCb);
        client.beforeNotify(secondCb);
        client.beforeNotify(secondCb);
        client.notify(new Throwable());
        assertEquals(2, callbackCount);
    }

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
