package com.bugsnag.android;

import static org.junit.Assert.assertEquals;

import com.bugsnag.android.Breadcrumb;
import com.bugsnag.android.BreadcrumbType;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@RunWith(MockitoJUnitRunner.class)
public class BreadcrumbSerializerTest {

    private Breadcrumb crumb;
    private Map<String, Object> metadata;

    /**
     * Generates a Breadcrumb for verifying the serializer
     */
    @Before
    public void setup() {
        Date timestamp = new Date(0);
        metadata = new HashMap<>();
        metadata.put("foo", "bar");
        crumb = new Breadcrumb("Whoops", BreadcrumbType.STATE, metadata,
                timestamp, NoopLogger.INSTANCE);
    }

    @Test
    public void serialize() {
        Map<String, Object> map = new HashMap<>();
        new BreadcrumbSerializer().serialize(map, crumb);
        assertEquals("1970-01-01T00:00:00.000Z", map.get("timestamp"));
        assertEquals("Whoops", map.get("message"));
        assertEquals("state", map.get("type"));
        assertEquals(metadata, map.get("metadata"));
    }
}
