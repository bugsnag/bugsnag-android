package com.bugsnag.android;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.List;

@SuppressWarnings("ConstantConditions")
public class ErrorFacadeTest {

    private InterceptingLogger logger;
    private Error error;
    private List<Stackframe> trace;

    /**
     * Sets up an error object
     */
    @Before
    public void setUp() {
        logger = new InterceptingLogger();
        trace = Collections.emptyList();
        ErrorInternal impl = new ErrorInternal("com.bar.CrashyClass",
                "Whoops", new Stacktrace(trace), ErrorType.ANDROID);
        error = new Error(impl, logger);
    }

    @Test
    public void errorClassValid() {
        assertEquals("com.bar.CrashyClass", error.getErrorClass());
        error.setErrorClass("com.Foo");
        assertEquals("com.Foo", error.getErrorClass());
    }

    @Test
    public void errorClassInvalid() {
        assertEquals("com.bar.CrashyClass", error.getErrorClass());
        error.setErrorClass(null);
        assertEquals("com.bar.CrashyClass", error.getErrorClass());
        assertNotNull(logger.getMsg());
    }

    @Test
    public void errorMessageValid() {
        assertEquals("Whoops", error.getErrorMessage());
        error.setErrorMessage("Oh dear oh dear");
        assertEquals("Oh dear oh dear", error.getErrorMessage());

        error.setErrorMessage(null);
        assertNull(error.getErrorMessage());
    }

    @Test
    public void typeValid() {
        assertEquals(ErrorType.ANDROID, error.getType());
        error.setType(ErrorType.REACTNATIVEJS);
        assertEquals(ErrorType.REACTNATIVEJS, error.getType());
    }

    @Test
    public void typeInvalid() {
        assertEquals(ErrorType.ANDROID, error.getType());
        error.setType(null);
        assertEquals(ErrorType.ANDROID, error.getType());
        assertNotNull(logger.getMsg());
    }

    @Test
    public void stacktraceValid() {
        assertEquals(trace, error.getStacktrace());
    }
}
