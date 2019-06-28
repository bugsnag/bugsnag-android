package com.bugsnag.android;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class ExceptionHandlerTest {

    private Context context;
    private Client client;

    /**
     * Sets the default exception handler to null to avoid any Bugsnag handlers created
     * in previous test
     *
     * @throws Exception if initialisation failed
     */
    @Before
    public void setUp() throws Exception {
        context = InstrumentationRegistry.getContext();
        // Start in a clean state, since we've created clients before in tests
        Thread.setDefaultUncaughtExceptionHandler(null);
        client = new Client(context, "api-key");
    }

    @After
    public void tearDown() {
        client.close();
    }

    @Test
    public void testEnableDisable() {
        assertTrue(Thread.getDefaultUncaughtExceptionHandler() instanceof ExceptionHandler);

        client.disableExceptionHandler();
        assertFalse(Thread.getDefaultUncaughtExceptionHandler() instanceof ExceptionHandler);
    }

    @Test
    public void testMultipleClients() {
        Client clientTwo = new Client(context, "client-two");
        Client clientThree = new Client(context, "client-two");
        clientThree.disableExceptionHandler();

        Thread.UncaughtExceptionHandler handler = Thread.getDefaultUncaughtExceptionHandler();
        assertTrue(handler instanceof ExceptionHandler);
        ExceptionHandler bugsnagHandler = (ExceptionHandler) handler;

        assertEquals(2, bugsnagHandler.clientMap.size());
    }

}
