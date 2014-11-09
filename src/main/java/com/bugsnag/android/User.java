package com.bugsnag.android;

class User implements JsonStream.Streamable {
    private String id;
    private String email;
    private String name;

    User(String id, String email, String name) {
        this.id = id;
        this.email = email;
        this.name = name;
    }

    public void toStream(JsonStream writer) {
        writer.object()
            .name("id").value(id)
            .name("email").value(email)
            .name("name").value(name)
        .endObject();
    }
}
