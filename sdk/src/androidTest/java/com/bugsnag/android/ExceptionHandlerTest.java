package com.bugsnag.android;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Date;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class ExceptionHandlerTest {

    private Context context;

    @Before
    public void setUp() throws Exception {
        context = InstrumentationRegistry.getContext();
        // Start in a clean state, since we've created clients before in tests
        Thread.setDefaultUncaughtExceptionHandler(null);
    }

    @Test
    public void testEnableDisable() {
        Client client = new Client(context, "api-key");
        assertTrue(Thread.getDefaultUncaughtExceptionHandler() instanceof ExceptionHandler);

        client.disableExceptionHandler();
        assertFalse(Thread.getDefaultUncaughtExceptionHandler() instanceof ExceptionHandler);
    }

    @Test
    public void testMultipleClients() {
        Client clientOne = new Client(context, "client-one");
        Client clientTwo = new Client(context, "client-two");
        Client clientThree = new Client(context, "client-two");
        clientThree.disableExceptionHandler();

        assertTrue(Thread.getDefaultUncaughtExceptionHandler() instanceof ExceptionHandler);
        ExceptionHandler bugsnagHandler = (ExceptionHandler) Thread.getDefaultUncaughtExceptionHandler();

        assertEquals(2, bugsnagHandler.clientMap.size());
    }

    @Test
    public void testIsCrashOnLaunch() throws Exception {
        ExceptionHandler handler = new ExceptionHandler(null);
        Date now = new Date();
        Client client = new Client(context, new Configuration("123"), now);

        assertTrue(handler.isCrashOnLaunch(client, now));

        client.config.setLaunchCrashThresholdMs(0);
        assertFalse(handler.isCrashOnLaunch(client, now));

        client.config.setLaunchCrashThresholdMs(10000);
        assertFalse(handler.isCrashOnLaunch(client, new Date(now.getTime() + 20000)));
    }

}
