package com.bugsnag.android;

import android.support.test.InstrumentationRegistry;
import android.support.test.filters.SmallTest;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;

@SmallTest
public class StrictModeTest {

    private static final String STRICT_MODE_MSG = "android.os.StrictMode$StrictModeViolation: policy=262146 violation=";
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
    public void testIsNotStrictModeThrowable() throws Exception {
        assertFalse(strictModeHandler.isStrictModeThrowable(new RuntimeException()));
        assertFalse(strictModeHandler.isStrictModeThrowable(new Throwable()));
    }

    @Test
    public void testIsStrictModeThrowable() throws Exception {
        Exception strictModeException = generateStrictModeException();

        if (strictModeException != null) {
            assertTrue(strictModeHandler.isStrictModeThrowable(strictModeException));

            RuntimeException wrappedException = new RuntimeException(strictModeException);
            assertTrue(strictModeHandler.isStrictModeThrowable(wrappedException));

            RuntimeException doubleWrappedException = new RuntimeException(wrappedException);
            assertTrue(strictModeHandler.isStrictModeThrowable(doubleWrappedException));
        }
    }

    @Test
    public void testStrictModeInvalidDesc() {
        String[] invalidArgs = {null, ""};

        for (String invalidArg : invalidArgs) {
            try {
                strictModeHandler.getViolationDescription(invalidArg);
                fail("Null/empty values not rejected");
            } catch (IllegalArgumentException ignored) {
            }
        }
    }

    @Test
    public void testStrictModeBadDesc() {
        String desc = strictModeHandler.getViolationDescription("Three blind mice, look how they run");
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

        if (exception != null) {
            String desc = strictModeHandler.getViolationDescription(exception.getMessage());
            assertEquals("DiskRead", desc);
        }
    }

    /**
     * Generates a StrictMode Exception (as it has private visibility in StrictMode)
     *
     * @return the StrictModeException. This is nullable as the errors StrictMode detect
     * depend on the API level.
     */
    private Exception generateStrictModeException() {
        try {
            violateStrictModePolicy();
        } catch (Exception e) {
            return e;
        }
        return null;
    }

    /**
     * Violate the strict mode by reading a file on the main thread
     */
    private void violateStrictModePolicy() {
        try {
            new FileWriter(new File(InstrumentationRegistry.getContext().getCacheDir(), "test")).write("test");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
