package com.bugsnag.android;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

public class RequestDeserializerTest {

    private Map<String, Object> map;

    /**
     * Generates a map for verifying the deserializer
     */
    @Before
    public void setup() {
        map = new HashMap<>();
        map.put("httpVersion", "HTTP/1.1");
        map.put("httpMethod", "GET");
        map.put("url", "https://example.com/api/users");
        map.put("body", "request body");
        map.put("bodyLength", 12L);

        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        headers.put("Authorization", "Bearer token");
        map.put("headers", headers);

        Map<String, String> params = new HashMap<>();
        params.put("id", "123");
        params.put("name", "test");
        map.put("params", params);
    }

    @Test
    public void deserialize() {
        Request request = new RequestDeserializer().deserialize(map);

        assertEquals("HTTP/1.1", request.getHttpVersion());
        assertEquals("GET", request.getHttpMethod());
        assertEquals("https://example.com/api/users", request.getUrl());
        assertEquals("request body", request.getBody());
        assertEquals(12L, request.getBodyLength());

        // Check headers
        assertEquals("application/json", request.getHeader("Content-Type"));
        assertEquals("Bearer token", request.getHeader("Authorization"));

        // Check query params
        assertEquals("123", request.getQueryParameter("id"));
        assertEquals("test", request.getQueryParameter("name"));
    }

    @Test
    public void deserializeWithNullValues() {
        Map<String, Object> nullMap = new HashMap<>();
        nullMap.put("httpVersion", null);
        nullMap.put("httpMethod", null);
        nullMap.put("url", null);

        Request request = new RequestDeserializer().deserialize(nullMap);

        assertNull(request.getHttpVersion());
        assertNull(request.getHttpMethod());
        assertEquals("", request.getUrl()); // setUrl with null sets empty string
    }

    @Test
    public void deserializeWithMissingOptionalFields() {
        Map<String, Object> minimalMap = new HashMap<>();
        minimalMap.put("httpMethod", "POST");
        minimalMap.put("url", "https://example.com/api");

        Request request = new RequestDeserializer().deserialize(minimalMap);

        assertNull(request.getHttpVersion());
        assertEquals("POST", request.getHttpMethod());
        assertEquals("https://example.com/api", request.getUrl());
        assertNull(request.getBody());
        assertEquals(-1L, request.getBodyLength()); // Default value
    }

    @Test
    public void deserializeWithEmptyMaps() {
        Map<String, Object> mapWithEmptyCollections = new HashMap<>();
        mapWithEmptyCollections.put("httpMethod", "GET");
        mapWithEmptyCollections.put("url", "https://example.com");
        mapWithEmptyCollections.put("headers", new HashMap<String, String>());
        mapWithEmptyCollections.put("params", new HashMap<String, String>());

        Request request = new RequestDeserializer().deserialize(mapWithEmptyCollections);

        assertNotNull(request);
        assertEquals("GET", request.getHttpMethod());
        assertEquals(0, request.getHeaderNames().size());
        assertEquals(0, request.getQueryParameterNames().size());
    }
}
