package com.bugsnag.android;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.io.IOException;

/**
 * Information about the current user of your application.
 */
class User implements JsonStream.Streamable {
    @Nullable private String id;
    @Nullable private String email;
    @Nullable private String name;

    User(Builder builder) {
        this.id = builder.id;
        this.email = builder.email;
        this.name = builder.name;
    }

    @Override
    public void toStream(@NonNull JsonStream writer) throws IOException {
        writer.beginObject();

        writer.name("id").value(id);
        writer.name("email").value(email);
        writer.name("name").value(name);

        writer.endObject();
    }

    @Nullable
    public String getId() {
        return id;
    }

    @Nullable
    public String getEmail() {
        return email;
    }

    @Nullable
    public String getName() {
        return name;
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Create a new user object based on a previous user object.
     *
     * @param user the previous user object.
     */
    public static Builder builder(User user) {
        return new Builder(user);
    }

    public static class Builder {
        @Nullable String id;
        @Nullable String email;
        @Nullable String name;

        public Builder() {
        }

        public Builder(User user) {
            id(user.getId());
            email(user.getEmail());
            name(user.getName());
        }

        /**
         * Set a unique identifier for the user currently using your application.
         * By default, this will be an automatically generated unique id
         * You can search for this information in your Bugsnag dashboard.
         *
         * @param id a unique identifier of the current user
         */
        public Builder id(@Nullable String id) {
            this.id = id;
            return this;
        }

        /**
         * Set the email address of the current user.
         * You can search for this information in your Bugsnag dashboard.
         *
         * @param email the email address of the current user
         */
        public Builder email(@Nullable String email) {
            this.email = email;
            return this;
        }

        /**
         * Set the name of the current user.
         * You can search for this information in your Bugsnag dashboard.
         *
         * @param name the name of the current user
         */
        public Builder name(@Nullable String name) {
            this.name = name;
            return this;
        }

        public User build() {
            return new User(this);
        }
    }
}
