package com.bugsnag.android;

abstract class CachedValue<V> extends SafeValue<V> {
    private V value;

    CachedValue(String name) {
        super(name);
    }

    V get() {
        if (value == null) {
            value = super.get();
        }
        return value;
    }

    abstract V calc() throws Exception;
}
