package com.bugsnag.android;

import java.util.Map;

class RequestDeserializer implements MapDeserializer<Request> {
    @Override
    public Request deserialize(Map<String, Object> map) {
        String httpVersion = MapUtils.getOrNull(map, "httpVersion");
        String httpMethod = MapUtils.getOrNull(map, "httpMethod");
        String url = MapUtils.getOrNull(map, "url");

        Request request = new Request(httpVersion, httpMethod, url);

        // Deserialize body
        String body = MapUtils.getOrNull(map, "body");
        if (body != null) {
            request.setBody(body);
        }

        // Deserialize bodyLength
        Long bodyLength = MapUtils.getLong(map, "bodyLength");
        if (bodyLength != null) {
            request.setBodyLength(bodyLength);
        }

        // Deserialize headers
        Map<String, String> headers = MapUtils.getOrNull(map, "headers");
        if (headers != null) {
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                request.addHeader(entry.getKey(), entry.getValue());
            }
        }

        // Deserialize query parameters
        Map<String, String> params = MapUtils.getOrNull(map, "params");
        if (params != null) {
            for (Map.Entry<String, String> entry : params.entrySet()) {
                request.addQueryParameter(entry.getKey(), entry.getValue());
            }
        }

        return request;
    }
}
