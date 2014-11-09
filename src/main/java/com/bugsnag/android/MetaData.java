package com.bugsnag.android;

import java.util.HashMap;
import java.util.Map;

public class MetaData implements JsonStream.Streamable {
    Map store = new HashMap();

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

    public MetaData copy() {
        // TODO
        return new MetaData();
    }

    public MetaData merge(MetaData source) {
        // TODO
        return this;
    }

    public MetaData filter(String[] filters) {
        // TODO
        return this;
    }

    public void toStream(JsonStream writer) {
        // TODO
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
