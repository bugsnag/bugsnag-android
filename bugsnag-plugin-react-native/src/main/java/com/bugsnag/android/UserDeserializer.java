package com.bugsnag.android;

import static com.bugsnag.android.MapUtils.getString;

import java.util.Map;

class UserDeserializer implements MapDeserializer<User> {
    @Override
    public User deserialize(Map<String, Object> map) {
        return new User(getString(map, "id"), getString(map, "email"), getString(map, "name"));
    }
}
