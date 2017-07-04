package com.bugsnag.android;

public class ClientRejectedExecutionTest extends BugsnagTestCase {

    private static final int MAX_ALLOWED_TASKS = 128;

    /**
     * Checks that an exception is not thrown when the max task queue is reached.
     */
    public void testRejectedExecution() {
        Client client = new Client(getContext(), "api-key");

        for (int k = 0; k < MAX_ALLOWED_TASKS * 2; k++) {
            client.notify(new Throwable());
        }
    }

}
