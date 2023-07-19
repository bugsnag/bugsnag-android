package com.bugsnag.android;

import static com.bugsnag.android.JsonUtilsKt.validateJson;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MetadataRedactionTest {

    @Test
    public void testBasicRedaction() throws IOException {
        Metadata metadata = new Metadata();
        metadata.setRedactedKeys(Collections.singleton(Pattern.compile(".*password.*")));
        metadata.addMetadata("example", "password", "p4ssw0rd");
        metadata.addMetadata("example", "confirm_password", "p4ssw0rd");
        metadata.addMetadata("example", "normal", "safe");
        verifyJsonRedacted(metadata, "metadata_redaction_0.json");
    }

    @Test
    public void testNestedRedaction() throws IOException {
        Map<String, String> sensitiveMap = new HashMap<>();
        sensitiveMap.put("password", "p4ssw0rd");
        sensitiveMap.put("confirm_password", "p4ssw0rd");
        sensitiveMap.put("normal", "safe");

        Metadata metadata = new Metadata();
        metadata.setRedactedKeys(Collections.singleton(Pattern.compile(".*password.*")));
        metadata.addMetadata("example", "sensitiveMap", sensitiveMap);
        verifyJsonRedacted(metadata, "metadata_redaction_1.json");
    }

    @Test
    public void testFilterConstructor() throws IOException {
        Metadata metadata = new Metadata();
        metadata.addMetadata("foo", "password", "abc123");
        verifyJsonRedacted(metadata, "metadata_redaction_2.json");
    }

    @Test
    public void testClearTab() throws IOException {
        Metadata metadata = new Metadata();
        metadata.addMetadata("example", "string", "value");
        metadata.clearMetadata("example");
        verifyJsonRedacted(metadata, "metadata_redaction_3.json");
    }

    @Test
    public void testDifferentRedactKeys() throws IOException {
        Metadata metadata = new Metadata();
        metadata.addMetadata("foo", "bar", "abc123");
        metadata.addMetadata("foo", "password", "abc123");
        metadata.setRedactedKeys(Collections.singleton(Pattern.compile("bar", Pattern.LITERAL)));
        verifyJsonRedacted(metadata, "metadata_redaction_4.json");
    }

    @Test
    public void testDefaultRedactKeys() {
        Metadata metadata = new Metadata();
        Matcher matcher = Pattern.compile("password", Pattern.LITERAL)
                .matcher(metadata.getRedactedKeys().toString());
        assertTrue(matcher.find());
    }

    private void verifyJsonRedacted(Metadata metadata, String resourceName) throws IOException {
        StringWriter writer = new StringWriter();
        JsonStream stream = new JsonStream(writer);
        metadata.toStream(stream);
        validateJson(resourceName, writer.toString());
    }
}
