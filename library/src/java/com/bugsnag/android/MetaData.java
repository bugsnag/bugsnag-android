package com.bugsnag.android;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

public class MetaData implements JsonStream.Streamable {
    Map<String, Object> store = new HashMap<String, Object>();

    public void toStream(JsonStream writer) {
        writer.value(store);
    }

    public void addToTab(String tabName, String key, Object value) {
        Map<String, Object> tab = getTab(tabName);
        if(value != null) {
            tab.put(key, value);
        } else {
            tab.remove(key);
        }
    }

    public void clearTab(String tabName) {
        store.remove(tabName);
    }

    public MetaData mergeAndFilter(MetaData overrides, String[] filters) {
        MetaData result = new MetaData();

        if(overrides == null) {
            overrides = new MetaData();
        }

        result.store = mergeAndFilter(this.store, overrides.store, filters);

        return result;
    }

    private Map<String, Object> getTab(String tabName) {
        Map<String, Object> tab = (Map<String, Object>)store.get(tabName);

        if(tab == null) {
            tab = new HashMap<String, Object>();
            store.put(tabName, tab);
        }

        return tab;
    }

    private static Map mergeAndFilter(Map<String, Object> base, Map<String, Object> overrides, String[] filters) {
        Map result = new HashMap();

        // Get a set of all possible keys in base and overrides
        Set<String> allKeys = new HashSet<String>();
        allKeys.addAll(base.keySet());
        allKeys.addAll(overrides.keySet());

        for(String key : allKeys) {
            Object baseValue = base.get(key);
            Object overridesValue = overrides.get(key);

            if(overridesValue != null) {
                if(baseValue != null && baseValue instanceof Map && overridesValue instanceof Map) {
                    // Both original and overrides are Maps, go deeper
                    result.put(key, mergeAndFilter((Map<String, Object>)baseValue, (Map<String, Object>)overridesValue, filters));
                } else {
                    result.put(key, filterObject(key, overridesValue, filters));
                }
            } else {
                // No collision, just use base value
                result.put(key, filterObject(key, baseValue, filters));
            }
        }

        // TODO: Recursive filtering

        return result;
    }

    private static Object filterObject(String key, Object obj, String[] filters) {
        if(filters == null || key == null) return obj;

        for(String filter : filters) {
            if(key.contains(filter)) {
                return "[FILTERED]";
            }
        }

        return obj;
    }
}
