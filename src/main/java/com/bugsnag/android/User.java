package com.bugsnag.android;

/**
 * Information about the current user of your application.
 */
class User implements JsonStream.Streamable {
    private String id;
    private String email;
    private String name;

    public void toStream(JsonStream writer) {
        writer.beginObject();

            if(id != null) {
                writer.name("id").value(id);
            }

            if(email != null) {
                writer.name("email").value(email);
            }

            if(name != null) {
                writer.name("name").value(name);
            }

        writer.endObject();
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setName(String name) {
        this.name = name;
    }
}
