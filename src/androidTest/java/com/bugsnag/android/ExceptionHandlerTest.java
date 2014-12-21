package com.bugsnag.android;

public class ExceptionHandlerTest extends BugsnagTestCase {
    public void testEnableDisable() {
        // Start in a clean state, since we've created clients before in tests
        Thread.setDefaultUncaughtExceptionHandler(null);

        Client client = new Client(getContext(), "api-key");
        assertTrue(Thread.getDefaultUncaughtExceptionHandler() instanceof ExceptionHandler);

        client.disableExceptionHandler();
        assertFalse(Thread.getDefaultUncaughtExceptionHandler() instanceof ExceptionHandler);
    }

    public void testMultipleClients() {
        // Start in a clean state, since we've created clients before in tests
        Thread.setDefaultUncaughtExceptionHandler(null);

        Client clientOne = new Client(getContext(), "client-one");
        Client clientTwo = new Client(getContext(), "client-two");
        Client clientThree = new Client(getContext(), "client-two");
        clientThree.disableExceptionHandler();

        assertTrue(Thread.getDefaultUncaughtExceptionHandler() instanceof ExceptionHandler);
        ExceptionHandler bugsnagHandler = (ExceptionHandler)Thread.getDefaultUncaughtExceptionHandler();

        assertEquals(2, bugsnagHandler.clientMap.size());
    }
}
