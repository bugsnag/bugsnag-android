package com.bugsnag.android;

import android.os.Build;
import android.os.StrictMode;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class StrictModeTest extends BugsnagTestCase {

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
