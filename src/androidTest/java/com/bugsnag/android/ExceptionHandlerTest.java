package com.bugsnag.android;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class ExceptionHandlerTest  {

    private Context context;

    @Before
    public void setUp() throws Exception {
        context = InstrumentationRegistry.getContext();
    }

    @Test
    public void testEnableDisable() {
        // Start in a clean state, since we've created clients before in tests
        Thread.setDefaultUncaughtExceptionHandler(null);

        Client client = new Client(context, "api-key");
        assertTrue(Thread.getDefaultUncaughtExceptionHandler() instanceof ExceptionHandler);

        client.disableExceptionHandler();
        assertFalse(Thread.getDefaultUncaughtExceptionHandler() instanceof ExceptionHandler);
    }

    @Test
    public void testMultipleClients() {
        // Start in a clean state, since we've created clients before in tests
        Thread.setDefaultUncaughtExceptionHandler(null);

        Client clientOne = new Client(context, "client-one");
        Client clientTwo = new Client(context, "client-two");
        Client clientThree = new Client(context, "client-two");
        clientThree.disableExceptionHandler();

        assertTrue(Thread.getDefaultUncaughtExceptionHandler() instanceof ExceptionHandler);
        ExceptionHandler bugsnagHandler = (ExceptionHandler)Thread.getDefaultUncaughtExceptionHandler();

        assertEquals(2, bugsnagHandler.clientMap.size());
    }
}
