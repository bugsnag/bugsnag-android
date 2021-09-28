package com.bugsnag.android;

import java.util.Map;

class UserDeserializer implements MapDeserializer<User> {
    @Override
    public User deserialize(Map<String, Object> map) {
        @SuppressWarnings({"unchecked", "rawtypes"}) Map<String, String> data = (Map) map;
        return new User(data);
    }
}
