package com.bugsnag.android;

import android.util.Pair;

import org.junit.Test;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

public class LifecycleBreadcrumbLoggerTest {

    private static final String FIRST_ACTIVITY = "MyActivity";
    private static final String SECOND_ACTIVITY = "SecondActivity";
    private static final String FIRST_CB = "onCreate";
    private static final String SECOND_CB = "onStart";

    @Test
    public void testLifecycleQueueing() throws Exception {
        LifecycleBreadcrumbLogger logger = new LifecycleBreadcrumbLogger(null);
        logger.leaveLifecycleBreadcrumb(FIRST_ACTIVITY, FIRST_CB);
        logger.leaveLifecycleBreadcrumb(SECOND_ACTIVITY, SECOND_CB);

        assertEquals(2, logger.queue.size());

        Pair<String, String> poll = logger.queue.poll();
        assertEquals(FIRST_ACTIVITY, poll.first);
        assertEquals(FIRST_CB, poll.second);

        poll = logger.queue.poll();
        assertEquals(SECOND_ACTIVITY, poll.first);
        assertEquals(SECOND_CB, poll.second);
    }

    @Test
    public void testLifecycleLogging() throws Exception {
        LifecycleBreadcrumbLogger logger = new LifecycleBreadcrumbLogger(BugsnagTestUtils.generateClient());
        logger.leaveLifecycleBreadcrumb(FIRST_ACTIVITY, FIRST_CB);
        logger.leaveLifecycleBreadcrumb(SECOND_ACTIVITY, SECOND_CB);
        assertTrue(logger.queue.isEmpty());
    }

}
