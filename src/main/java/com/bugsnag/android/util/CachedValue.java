package com.bugsnag.android;

abstract public class CachedValue<V> {
    private V value;

    public V get() {
        if (value == null) {
            try {
                value = calc();
            } catch(Exception e) {
                Logger.warn("Could not get value", e);
            }
        }
        return value;
    }

    public abstract V calc() throws Exception;
}
