package com.bugsnag.android;

import android.support.annotation.NonNull;

import java.io.IOException;

/**
 * Information about the current user of your application.
 */
class User implements JsonStream.Streamable {
    private String id;
    private String email;
    private String name;

    User() {
    }

    User(String id, String email, String name) {
        this.id = id;
        this.email = email;
        this.name = name;
    }

    User(@NonNull User u) {
        this(u.id, u.email, u.name);
    }

    @Override
    public void toStream(@NonNull JsonStream writer) throws IOException {
        writer.beginObject();

        writer.name("id").value(id);
        writer.name("email").value(email);
        writer.name("name").value(name);

        writer.endObject();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
