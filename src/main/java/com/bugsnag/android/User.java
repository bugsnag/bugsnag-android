package com.bugsnag.android;

class User implements JsonStream.Streamable {
    private String id;
    private String email;
    private String name;

    public void toStream(JsonStream writer) {
        writer.beginObject()
            .name("id").value(id)
            .name("email").value(email)
            .name("name").value(name)
        .endObject();
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
