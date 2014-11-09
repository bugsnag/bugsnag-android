package com.bugsnag.android;

abstract class CachedValue<V> extends SafeValue<V> {
    private V value;

    CachedValue(String name) {
        super(name);
    }

    public V get() {
        if (value == null) {
            value = super.get();
        }
        return value;
    }

    public abstract V calc() throws Exception;
}
