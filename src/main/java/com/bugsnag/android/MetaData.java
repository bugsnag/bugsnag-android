package com.bugsnag.android;

import java.util.HashMap;
import java.util.Map;

public class MetaData implements JsonStream.Streamable {
    Map<String, Object> store = new HashMap<String, Object>();

    public void toStream(JsonStream writer) {
        writer.value(store);
    }

    public void addToTab(String tabName, String key, Object value) {
        Map tab = getTab(tabName);
        if(value != null) {
            tab.put(key, value);
        } else {
            tab.remove(key);
        }
    }

    public void clearTab(String tabName) {
        store.remove(tabName);
    }

    private Map getTab(String tabName) {
        Object tab = store.get(tabName);

        if(tab == null || !(tab instanceof Map)) {
            tab = new HashMap();
            store.put(tabName, tab);
        }

        return (Map)tab;
    }
}
