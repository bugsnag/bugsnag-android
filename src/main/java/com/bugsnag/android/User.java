package com.bugsnag.android;

class User implements JsonStreamer.Streamable {
    private String id;
    private String email;
    private String name;

    public User(String id, String email, String name) {
        this.id = id;
        this.email = email;
        this.name = name;
    }

    public void toStream(JsonStreamer writer) {
        writer.beginObject()
            .name("id").value(id)
            .name("email").value(email)
            .name("name").value(name)
        .endObject();
    }
}
