package com.bugsnag.android;

import java.util.Map;

class ResponseDeserializer implements MapDeserializer<Response> {
    @Override
    public Response deserialize(Map<String, Object> map) {
        Integer statusCode = MapUtils.getInt(map, "statusCode");
        if (statusCode == null) {
            statusCode = 0;
        }

        Response response = new Response(statusCode);

        // Deserialize body
        String body = MapUtils.getOrNull(map, "body");
        if (body != null) {
            response.setBody(body);
        }

        // Deserialize bodyLength
        Long bodyLength = MapUtils.getLong(map, "bodyLength");
        if (bodyLength != null) {
            response.setBodyLength(bodyLength);
        }

        // Deserialize headers
        Map<String, String> headers = MapUtils.getOrNull(map, "headers");
        if (headers != null) {
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                response.addHeader(entry.getKey(), entry.getValue());
            }
        }

        return response;
    }
}
