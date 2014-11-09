package com.bugsnag.android;

abstract class SafeValue<V> {
    public V get() {
        V value = null;
        try {
            value = calc();
        } catch(Exception e) {
            Logger.warn("Could not get value", e);
        }
        return value;
    }

    public abstract V calc() throws Exception;
}
