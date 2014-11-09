package com.bugsnag.android;

abstract class SafeValue<V> {
    private String name;

    SafeValue(String name) {
        this.name = name;
    }

    public V get() {
        V value = null;
        try {
            value = calc();
        } catch(Exception e) {
            Logger.warn(String.format("Could not get value '%s'", name), e);
        }
        return value;
    }

    public abstract V calc() throws Exception;
}
