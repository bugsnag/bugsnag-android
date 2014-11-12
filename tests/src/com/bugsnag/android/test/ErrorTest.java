package com.bugsnag.android;

import android.test.AndroidTestCase;

import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;

import com.bugsnag.android.Configuration;
import com.bugsnag.android.Error;

public class ErrorTest extends AndroidTestCase {
    @Override
    protected void setUp() {

    }

    public void testShouldIgnore() {
        Configuration config = new Configuration("api-key");
        config.ignoreClasses = new String[] {"java.io.IOException"};

        // Shouldn't ignore classes not in ignoreClasses
        Error error = new Error(config, new RuntimeException("Test"));
        assertFalse(error.shouldIgnore());

        // Should ignore errors in ignoreClasses
        error = new Error(config, new java.io.IOException("Test"));
        assertTrue(error.shouldIgnore());
    }

    public void testGetExceptionName() {
        Configuration config = new Configuration("api-key");

        Error error = new Error(config, new RuntimeException("Test"));
        assertEquals(error.getExceptionName(), "java.lang.RuntimeException");
    }

    public void testGetExceptionMessage() {
        Configuration config = new Configuration("api-key");

        Error error = new Error(config, new RuntimeException("Example message"));
        assertEquals(error.getExceptionMessage(), "Example message");
    }
}
