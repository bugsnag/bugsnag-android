package com.bugsnag.android;

import static com.bugsnag.android.BugsnagTestUtils.streamableToJson;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import androidx.test.filters.SmallTest;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

@SmallTest
public class MetadataRedactionTest {

    @Test
    public void testBasicRedaction() throws JSONException, IOException {
        Metadata metadata = new Metadata();
        metadata.setRedactKeys(Collections.singleton("password"));
        metadata.addMetadata("example", "password", "p4ssw0rd");
        metadata.addMetadata("example", "confirm_password", "p4ssw0rd");
        metadata.addMetadata("example", "normal", "safe");

        JSONObject metadataJson = streamableToJson(metadata);
        assertTrue(metadataJson.has("example"));

        JSONObject tabJson = metadataJson.getJSONObject("example");
        assertEquals("[REDACTED]", tabJson.getString("password"));
        assertEquals("[REDACTED]", tabJson.getString("confirm_password"));
        assertEquals("safe", tabJson.getString("normal"));
    }

    @Test
    public void testNestedRedaction() throws JSONException, IOException {
        Map<String, String> sensitiveMap = new HashMap<>();
        sensitiveMap.put("password", "p4ssw0rd");
        sensitiveMap.put("confirm_password", "p4ssw0rd");
        sensitiveMap.put("normal", "safe");

        Metadata metadata = new Metadata();
        metadata.setRedactKeys(Collections.singleton("password"));
        metadata.addMetadata("example", "sensitiveMap", sensitiveMap);

        JSONObject metadataJson = streamableToJson(metadata);
        assertTrue(metadataJson.has("example"));

        JSONObject tabJson = metadataJson.getJSONObject("example");
        JSONObject sensitiveMapJson = tabJson.getJSONObject("sensitiveMap");
        assertEquals("[REDACTED]", sensitiveMapJson.getString("password"));
        assertEquals("[REDACTED]", sensitiveMapJson.getString("confirm_password"));
        assertEquals("safe", sensitiveMapJson.getString("normal"));
    }

    @Test
    public void testFilterConstructor() throws Exception {
        Metadata metadata = new Metadata();
        metadata.addMetadata("foo", "password", "abc123");
        JSONObject jsonObject = streamableToJson(metadata);

        assertEquals(Collections.singleton("password"), metadata.getRedactKeys());
        assertEquals("[REDACTED]", jsonObject.getJSONObject("foo").get("password"));
    }

    @Test
    public void testClearTab() throws Exception {
        Metadata metadata = new Metadata();
        metadata.addMetadata("example", "string", "value");
        metadata.clearMetadata("example", null);
        JSONObject json = streamableToJson(metadata);
        assertFalse(json.has("example"));
    }
}
