package com.bugsnag.android;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Before;
import org.junit.Test;

import java.util.Date;

@SuppressWarnings("ConstantConditions")
public class SessionFacadeTest {

    private Session session;
    private InterceptingLogger logger;

    /**
     * Captures session logs
     */
    @Before
    public void setUp() {
        logger = new InterceptingLogger();
        session = new Session(
                "123", new Date(0), new User(),
                true, new Notifier(), logger, "BUGSNAG_API_KEY"
        );
    }

    @Test
    public void validId() {
        assertEquals("123", session.getId());
        session.setId("456");
        assertEquals("456", session.getId());
    }

    @Test
    public void invalidId() {
        assertEquals("123", session.getId());
        session.setId(null);
        assertEquals("123", session.getId());
        assertNotNull(logger.getMsg());
    }

    @Test
    public void validStartedAt() {
        assertEquals(new Date(0).getTime(), session.getStartedAt().getTime());
        session.setStartedAt(new Date(5));
        assertEquals(new Date(5).getTime(), session.getStartedAt().getTime());
    }

    @Test
    public void invalidStartedAt() {
        assertEquals(new Date(0).getTime(), session.getStartedAt().getTime());
        session.setStartedAt(null);
        assertEquals(new Date(0).getTime(), session.getStartedAt().getTime());
        assertNotNull(logger.getMsg());
    }
}
