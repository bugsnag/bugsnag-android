package com.bugsnag.android;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeNotNull;

import android.content.Context;
import android.os.Build;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.filters.SmallTest;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

@SmallTest
public class StrictModeTest {

    private static final String STRICT_MODE_MSG = "android.os.StrictMode"
        + "$StrictModeViolation: policy=262146 violation=";
    private final StrictModeHandler strictModeHandler = new StrictModeHandler();

    @Before
    public void setUp() throws Exception {
        StrictModeWrapper.setUp();
    }

    @After
    public void tearDown() throws Exception {
        StrictModeWrapper.tearDown();
    }

    @Test
    public void testIsNotStrictModeThrowable() {
        assertFalse(strictModeHandler.isStrictModeThrowable(new RuntimeException()));
        assertFalse(strictModeHandler.isStrictModeThrowable(new Throwable()));
    }

    @Test
    public void testIsStrictModeThrowable() {
        Exception strictModeException = generateStrictModeException();

        assumeNotNull(strictModeException);

        assertTrue(strictModeHandler.isStrictModeThrowable(strictModeException));

        RuntimeException wrappedException = new RuntimeException(strictModeException);
        assertTrue(strictModeHandler.isStrictModeThrowable(wrappedException));

        RuntimeException doubleWrappedException = new RuntimeException(wrappedException);
        assertTrue(strictModeHandler.isStrictModeThrowable(doubleWrappedException));

    }

    @Test
    public void testStrictModeInvalidDesc() {
        String[] invalidArgs = {null, ""};

        for (String invalidArg : invalidArgs) {
            try {
                strictModeHandler.getViolationDescription(invalidArg);
                fail("Null/empty values not rejected");
            } catch (IllegalArgumentException ignored) {
                assertNotNull(ignored);
            }
        }
    }

    @Test
    public void testStrictModeBadDesc() {
        String msg = "Three blind mice, look how they run";
        String desc = strictModeHandler.getViolationDescription(msg);
        assertNull(desc);

        String nonNumeric = strictModeHandler.getViolationDescription("violation=5abc");
        assertNull(nonNumeric);
    }

    @Test
    public void testStrictModeDesc() {
        String fileReadDesc = strictModeHandler.getViolationDescription(STRICT_MODE_MSG + "2");
        assertEquals("DiskRead", fileReadDesc);

        String fileWriteDesc = strictModeHandler.getViolationDescription(STRICT_MODE_MSG + "1");
        assertEquals("DiskWrite", fileWriteDesc);
    }

    @Test
    public void testStrictModeDescException() {
        Exception exception = generateStrictModeException();
        assumeNotNull(exception);

        String desc = strictModeHandler.getViolationDescription(exception.getMessage());

        String expected;
        if (Build.VERSION.SDK_INT >= 28) {
            // the violation description format changed to be more generic in P,
            // no longer possible to get a full description
            expected = null;
        } else {
            expected = "DiskRead";
        }
        assertEquals(expected, desc);
    }

    /**
     * Generates a StrictMode Exception (as it has private visibility in StrictMode)
     *
     * @return a nullable StrictModeException
     */
    private Exception generateStrictModeException() {
        try {
            violateStrictModePolicy();
        } catch (Exception exception) {
            return exception;
        }
        return null;
    }

    /**
     * Violate the strict mode by reading a file on the main thread
     */
    private void violateStrictModePolicy() {
        try {
            Context context = ApplicationProvider.getApplicationContext();
            new FileWriter(new File(context.getCacheDir(), "test")).write("test");
        } catch (IOException exception) {
            exception.printStackTrace();
        }
    }

}
