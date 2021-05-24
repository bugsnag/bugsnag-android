package com.bugsnag.android;

import static com.bugsnag.android.BreadcrumbType.MANUAL;
import static com.bugsnag.android.BreadcrumbType.NAVIGATION;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("ConstantConditions") // suppress warning about making redundant null checks
public class BreadcrumbFacadeTest {

    private Breadcrumb crumb;
    private InterceptingLogger logger;
    private HashMap<String, Object> metadata;

    /**
     * Constructs a Breadcrumb wrapper object
     */
    @Before
    public void setUp() {
        metadata = new HashMap<>();
        logger = new InterceptingLogger();
        crumb = new Breadcrumb("Panini", NAVIGATION, metadata, new Date(0), logger);
    }

    @Test
    public void messageValid() {
        assertEquals("Panini", crumb.getMessage());
        crumb.setMessage("Croissant");
        assertEquals("Croissant", crumb.getMessage());
    }

    @Test
    public void messageInvalid() {
        assertEquals("Panini", crumb.getMessage());
        crumb.setMessage(null);
        assertEquals("Panini", crumb.getMessage());
        assertNotNull(logger.getMsg());
    }

    @Test
    public void typeValid() {
        assertEquals(NAVIGATION, crumb.getType());
        crumb.setType(MANUAL);
        assertEquals(MANUAL, crumb.getType());
    }

    @Test
    public void typeInvalid() {
        assertEquals(NAVIGATION, crumb.getType());
        crumb.setType(null);
        assertEquals(NAVIGATION, crumb.getType());
        assertNotNull(logger.getMsg());
    }

    @Test
    public void metadataValid() {
        assertEquals(metadata, crumb.getMetadata());
        Map<String, Object> map = Collections.<String, Object>singletonMap("foo", true);
        crumb.setMetadata(map);
        assertEquals(map, crumb.getMetadata());
        crumb.setMetadata(null);
        assertNull(crumb.getMetadata());
    }

    @Test
    public void dateValid() {
        assertEquals(new Date(0).getTime(), crumb.getTimestamp().getTime());
    }

    @Test
    public void stringDateValid() {
        assertEquals("1970-01-01T00:00:00.000Z", crumb.getStringTimestamp());
    }
}
