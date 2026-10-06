package com.bugsnag.android;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

public class ResponseDeserializerTest {

    private Map<String, Object> map;

    /**
     * Generates a map for verifying the deserializer
     */
    @Before
    public void setup() {
        map = new HashMap<>();
        map.put("statusCode", 200);
        map.put("body", "response body");
        map.put("bodyLength", 13L);

        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        headers.put("Cache-Control", "no-cache");
        map.put("headers", headers);
    }

    @Test
    public void deserialize() {
        Response response = new ResponseDeserializer().deserialize(map);

        assertEquals(200, response.getStatusCode());
        assertEquals("response body", response.getBody());
        assertEquals(13L, response.getBodyLength());

        // Check headers
        assertEquals("application/json", response.getHeader("Content-Type"));
        assertEquals("no-cache", response.getHeader("Cache-Control"));
    }

    @Test
    public void deserializeWithNullStatusCode() {
        Map<String, Object> nullMap = new HashMap<>();
        nullMap.put("statusCode", null);
        nullMap.put("body", "test body");

        Response response = new ResponseDeserializer().deserialize(nullMap);

        assertEquals(0, response.getStatusCode()); // Default value when null
        assertEquals("test body", response.getBody());
    }

    @Test
    public void deserializeWithMissingOptionalFields() {
        Map<String, Object> minimalMap = new HashMap<>();
        minimalMap.put("statusCode", 404);

        Response response = new ResponseDeserializer().deserialize(minimalMap);

        assertEquals(404, response.getStatusCode());
        assertNull(response.getBody());
        assertEquals(-1L, response.getBodyLength()); // Default value
    }

    @Test
    public void deserializeWithEmptyHeaders() {
        Map<String, Object> mapWithEmptyHeaders = new HashMap<>();
        mapWithEmptyHeaders.put("statusCode", 500);
        mapWithEmptyHeaders.put("headers", new HashMap<String, String>());

        Response response = new ResponseDeserializer().deserialize(mapWithEmptyHeaders);

        assertNotNull(response);
        assertEquals(500, response.getStatusCode());
        assertEquals(0, response.getHeaderNames().size());
    }

    @Test
    public void deserializeWithVariousStatusCodes() {
        Map<String, Object> successMap = new HashMap<>();
        successMap.put("statusCode", 201);
        assertEquals(201, new ResponseDeserializer().deserialize(successMap).getStatusCode());

        Map<String, Object> redirectMap = new HashMap<>();
        redirectMap.put("statusCode", 302);
        assertEquals(302, new ResponseDeserializer().deserialize(redirectMap).getStatusCode());

        Map<String, Object> errorMap = new HashMap<>();
        errorMap.put("statusCode", 503);
        assertEquals(503, new ResponseDeserializer().deserialize(errorMap).getStatusCode());
    }

    @Test
    public void deserializeWithBodyLength() {
        Map<String, Object> mapWithBodyLength = new HashMap<>();
        mapWithBodyLength.put("statusCode", 200);
        mapWithBodyLength.put("body", "Hello World");
        mapWithBodyLength.put("bodyLength", 11L);

        Response response = new ResponseDeserializer().deserialize(mapWithBodyLength);

        assertEquals("Hello World", response.getBody());
        assertEquals(11L, response.getBodyLength());
    }
}
