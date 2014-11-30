package com.bugsnag.android;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class MetaData implements JsonStream.Streamable {
    Map<String, Object> store = new HashMap<String, Object>();
    String[] filters;

    public void toStream(JsonStream writer) {
        objectToStream(store, writer);
    }

    public void addToTab(String tabName, Map<String, Object> tab) {
        if(tab != null) {
            store.put(tabName, tab);
        } else {
            clearTab(tabName);
        }
    }

    public void addToTab(String tabName, String key, Object value) {
        getTab(tabName).put(key, value);
    }

    public void clearTab(String tabName) {
        store.remove(tabName);
    }

    public MetaData merge(MetaData overrides) {
        MetaData result = new MetaData();

        if(overrides == null) {
            overrides = new MetaData();
        }

        result.store = mergeMaps(this.store, overrides.store);

        return result;
    }

    public MetaData filter(String[] filters) {
        this.filters = filters;

        return this;
    }

    private Map<String, Object> getTab(String tabName) {
        Map<String, Object> tab = (Map<String, Object>)store.get(tabName);

        if(tab == null) {
            tab = new HashMap<String, Object>();
            store.put(tabName, tab);
        }

        return tab;
    }

    private static Map mergeMaps(Map<String, Object> base, Map<String, Object> overrides) {
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
                    result.put(key, mergeMaps((Map<String, Object>)baseValue, (Map<String, Object>)overridesValue));
                } else {
                    result.put(key, overridesValue);
                }
            } else {
                // No collision, just use base value
                result.put(key, baseValue);
            }
        }

        return result;
    }

    private void objectToStream(Object obj, JsonStream writer) {
        if(obj == null) {
            writer.nullValue();
        } else if(obj instanceof String) {
            writer.value((String)obj);
        } else if(obj instanceof Number) {
            writer.value((Number)obj);
        } else if(obj instanceof Boolean) {
            writer.value((Boolean)obj);
        } else if(obj instanceof Map) {
            // Map objects
            writer.beginObject();
            for(Iterator entries = ((Map)obj).entrySet().iterator(); entries.hasNext();) {
                Map.Entry entry = (Map.Entry)entries.next();
                Object keyObj = entry.getKey();
                if(keyObj instanceof String) {
                    String key = (String)keyObj;
                    writer.name(key);
                    if(shouldFilter(key)) {
                        writer.value("[FILTERED]");
                    } else {
                        objectToStream(entry.getValue(), writer);
                    }
                }
            }
            writer.endObject();
        } else if(obj instanceof Collection) {
            // Collection objects (Lists, Sets etc)
            writer.beginArray();
            for(Object entry : (Collection)obj) {
                objectToStream(entry, writer);
            }
            writer.endArray();
        } else if(obj.getClass().isArray()) {
            // Primitive array objects
            writer.beginArray();
            int length = Array.getLength(obj);
            for (int i = 0; i < length; i += 1) {
                objectToStream(Array.get(obj, i), writer);
            }
            writer.endArray();
        } else {
            writer.value("[OBJECT]");
        }
    }

    private boolean shouldFilter(String key) {
        if(filters == null || key == null) return false;

        for(String filter : filters) {
            if(key.contains(filter)) {
                return true;
            }
        }

        return false;
    }
}
