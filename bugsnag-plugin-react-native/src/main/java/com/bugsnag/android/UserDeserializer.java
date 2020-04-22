package com.bugsnag.android;

import java.util.Map;

class UserDeserializer implements MapDeserializer<User> {
    @Override
    public User deserialize(Map<String, Object> map) {
        return new User(
                MapUtils.<String>getOrNull(map, "id"),
                MapUtils.<String>getOrNull(map, "email"),
                MapUtils.<String>getOrNull(map, "name")
        );
    }
}
