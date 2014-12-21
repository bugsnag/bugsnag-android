package com.bugsnag.android;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

class Breadcrumbs implements JsonStream.Streamable {
    private static class Breadcrumb {
        private static final int MAX_MESSAGE_LENGTH = 140;
        private static DateFormat iso8601;
        private String timestamp;
        private String message;

        static {
            TimeZone tz = TimeZone.getTimeZone("UTC");
            iso8601 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mmZ");
            iso8601.setTimeZone(tz);
        }

        Breadcrumb(String message) {
            this.timestamp = iso8601.format(new Date());
            this.message = message.substring(0, Math.min(message.length(), MAX_MESSAGE_LENGTH));
        }
    }

    private static final int DEFAULT_MAX_SIZE = 20;
    private List<Breadcrumb> store = new ArrayList<Breadcrumb>();
    private int maxSize = DEFAULT_MAX_SIZE;

    public void toStream(JsonStream writer) {
        writer.beginArray();

        for(Breadcrumb breadcrumb : store) {
            writer.beginArray();
            writer.value(breadcrumb.timestamp);
            writer.value(breadcrumb.message);
            writer.endArray();
        }

        writer.endArray();
    }

    void add(String message) {
        if(store.size() >= maxSize) {
            store.remove(0);
        }

        store.add(new Breadcrumb(message));
    }

    void clear() {
        store.clear();
    }

    void setSize(int size) {
        if(size > store.size()) {
            this.maxSize = size;
        } else {
            store.subList(0, store.size() - size).clear();
        }
    }
}
