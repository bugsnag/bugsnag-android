package com.bugsnag.android;

import static org.junit.Assert.assertEquals;

import com.bugsnag.android.AppWithState;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@RunWith(MockitoJUnitRunner.class)
public class AppSerializerTest {

    private AppWithState app;

    /**
     * Generates an AppWithState for verifying the serializer
     */
    @Before
    public void setup() throws IOException {
        app = new AppWithState(TestData.generateConfig(),
                "x86", "com.example.foo", "prod",
                "1.5.3", "code-id-123", 509, 23, true, true
        );
    }

    @Test
    public void serialize() {
        Map<String, Object> map = new HashMap<>();
        new AppSerializer().serialize(map, app);

        assertEquals(509, map.get("duration"));
        assertEquals(23, map.get("durationInForeground"));
        assertEquals(true, map.get("inForeground"));
        assertEquals(true, map.get("isLaunching"));
        assertEquals("x86", map.get("binaryArch"));
        assertEquals("builduuid-123", map.get("buildUuid"));
        assertEquals("code-id-123", map.get("codeBundleId"));
        assertEquals("com.example.foo", map.get("id"));
        assertEquals("prod", map.get("releaseStage"));
        assertEquals("android", map.get("type"));
        assertEquals("1.5.3", map.get("version"));
        assertEquals(55, map.get("versionCode"));
    }
}
