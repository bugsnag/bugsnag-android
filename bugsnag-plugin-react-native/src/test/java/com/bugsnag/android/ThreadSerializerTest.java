package com.bugsnag.android;

import static org.junit.Assert.assertEquals;

import com.bugsnag.android.NoopLogger;
import com.bugsnag.android.Stacktrace;
import com.bugsnag.android.Thread;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RunWith(MockitoJUnitRunner.class)
public class ThreadSerializerTest {

    private Thread thread;
    private Map<String, Object> frame;

    /**
     * Generates a Thread for verifying the serializer
     */
    @Before
    public void setup() {
        frame = new HashMap<>();
        frame.put("method", "foo()");
        frame.put("file", "Bar.kt");
        frame.put("lineNumber", 55);
        frame.put("inProject", true);

        Stackframe stackframe = new Stackframe("foo()", "Bar.kt", 55, true);
        List<Stackframe> frames = new ArrayList<>(Collections.singletonList(stackframe));
        Stacktrace stacktrace = new Stacktrace(frames);
        thread = new Thread("1", "fake-thread", ErrorType.ANDROID,
                true, Thread.State.RUNNABLE, stacktrace, NoopLogger.INSTANCE);
    }

    @Test
    public void serialize() {
        Map<String, Object> map = new HashMap<>();
        new ThreadSerializer().serialize(map, thread);

        assertEquals("1", map.get("id"));
        assertEquals("fake-thread", map.get("name"));
        assertEquals("android", map.get("type"));
        assertEquals(true, map.get("errorReportingThread"));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> frames = (List<Map<String, Object>>) map.get("stacktrace");
        assertEquals(1, frames.size());

        Map<String, Object> data = frames.get(0);
        assertEquals(frame, data);
    }
}
