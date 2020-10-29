package com.bugsnag.android;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@SuppressWarnings("ConstantConditions")
public class ThreadFacadeTest {

    private Thread thread;
    private InterceptingLogger logger;
    private Stacktrace stacktrace;

    /**
     * Constructs a Thread wrapper object
     */
    @Before
    public void setUp() {
        logger = new InterceptingLogger();
        List<Stackframe> frames = Collections.emptyList();
        stacktrace = new Stacktrace(frames);
        thread = new Thread(1, "thread-2", ThreadType.ANDROID, false, stacktrace, logger);
    }

    @Test
    public void idValid() {
        assertEquals(1, thread.getId());
        thread.setId(55);
        assertEquals(55, thread.getId());
    }

    @Test
    public void nameValid() {
        assertEquals("thread-2", thread.getName());
        thread.setName("foo");
        assertEquals("foo", thread.getName());
    }

    @Test
    public void nameInvalid() {
        assertEquals("thread-2", thread.getName());
        thread.setName(null);
        assertEquals("thread-2", thread.getName());
        assertNotNull(logger.getMsg());
    }

    @Test
    public void typeValid() {
        assertEquals(ThreadType.ANDROID, thread.getType());
        thread.setType(ThreadType.REACTNATIVEJS);
        assertEquals(ThreadType.REACTNATIVEJS, thread.getType());
    }

    @Test
    public void typeInvalid() {
        assertEquals(ThreadType.ANDROID, thread.getType());
        thread.setType(null);
        assertEquals(ThreadType.ANDROID, thread.getType());
        assertNotNull(logger.getMsg());
    }

    @Test
    public void errorReportingThreadValid() {
        assertFalse(thread.getErrorReportingThread());
    }

    @Test
    public void stacktraceValid() {
        assertEquals(stacktrace.getTrace(), thread.getStacktrace());
        List<Stackframe> frames = Collections.emptyList();
        Stacktrace other = new Stacktrace(frames);
        thread.setStacktrace(other.getTrace());
        assertEquals(other.getTrace(), thread.getStacktrace());
    }

    @Test
    public void stacktraceInvalid() {
        assertEquals(stacktrace.getTrace(), thread.getStacktrace());
        thread.setStacktrace(null);
        assertEquals(stacktrace.getTrace(), thread.getStacktrace());
        assertNotNull(logger.getMsg());
    }
}
