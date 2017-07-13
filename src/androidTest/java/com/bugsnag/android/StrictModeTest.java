package com.bugsnag.android;

import android.os.Build;
import android.os.StrictMode;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class StrictModeTest extends BugsnagTestCase {

    public static final String STRICT_MODE_MSG = "android.os.StrictMode$StrictModeViolation: policy=262146 violation=";
    private final StrictModeHandler strictModeHandler = new StrictModeHandler();

    @Override
    public void setUp() throws Exception {
        super.setUp();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
            StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
                .detectDiskReads()
                .penaltyDeath()
                .build());
        }
    }

    @Override
    public void tearDown() throws Exception {
        super.tearDown();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
            StrictMode.setThreadPolicy(StrictMode.ThreadPolicy.LAX);
        }
    }

    public void testIsNotStrictModeThrowable() throws Exception {
        assertFalse(strictModeHandler.isStrictModeThrowable(new RuntimeException()));
        assertFalse(strictModeHandler.isStrictModeThrowable(new Throwable()));
    }

    public void testIsStrictModeThrowable() throws Exception {
        Exception strictModeException = generateStrictModeException();
        assertTrue(strictModeHandler.isStrictModeThrowable(strictModeException));

        RuntimeException wrappedException = new RuntimeException(strictModeException);
        assertTrue(strictModeHandler.isStrictModeThrowable(wrappedException));

        RuntimeException doubleWrappedException = new RuntimeException(wrappedException);
        assertTrue(strictModeHandler.isStrictModeThrowable(doubleWrappedException));
    }

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

    public void testStrictModeBadDesc() {
        String desc = strictModeHandler.getViolationDescription("Three blind mice, look how they run");
        assertNull(desc);

        String nonNumeric = strictModeHandler.getViolationDescription("violation=5abc");
        assertNull(nonNumeric);
    }

    public void testStrictModeDesc() {
        String fileReadDesc = strictModeHandler.getViolationDescription(STRICT_MODE_MSG + "2");
        assertEquals("DiskRead (2)", fileReadDesc);

        String fileWriteDesc = strictModeHandler.getViolationDescription(STRICT_MODE_MSG + "1");
        assertEquals("DiskWrite (1)", fileWriteDesc);
    }

    /**
     * Generates a StrictMode Exception (as it has private visibility in StrictMode)
     *
     * @return the StrictModeException
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
            new FileWriter(new File(getContext().getCacheDir(), "test")).write("test");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
