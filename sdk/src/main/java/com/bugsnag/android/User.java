package com.bugsnag.android;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.io.IOException;

/**
 * Information about the current user of your application.
 */
class User implements JsonStream.Streamable {

    @Nullable
    private String id;

    @Nullable
    private String email;

    @Nullable
    private String name;

    User() {
    }

    User(@Nullable String id, @Nullable String email, @Nullable String name) {
        this.id = id;
        this.email = email;
        this.name = name;
    }

    User(@NonNull User user) {
        this(user.id, user.email, user.name);
    }

    @Override
    public void toStream(@NonNull JsonStream writer) throws IOException {
        writer.beginObject();

        writer.name("id").value(id);
        writer.name("email").value(email);
        writer.name("name").value(name);

        writer.endObject();
    }

    /**
     * @return the user ID, by default a UUID generated on installation
     */
    @Nullable
    public String getId() {
        return id;
    }

    /**
     * Overrides the default user ID
     *
     * @param id the new ID
     */
    public void setId(@Nullable String id) {
        this.id = id;
    }

    /**
     * @return the user's email, if available
     */
    @Nullable
    public String getEmail() {
        return email;
    }

    /**
     * Sets the user's email
     *
     * @param email the user email
     */
    public void setEmail(@Nullable String email) {
        this.email = email;
    }

    /**
     * @return the user's name, if available
     */
    @Nullable
    public String getName() {
        return name;
    }

    /**
     * Sets the user's name
     *
     * @param name the user name
     */
    public void setName(@Nullable String name) {
        this.name = name;
    }
}
