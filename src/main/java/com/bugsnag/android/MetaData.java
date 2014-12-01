package com.bugsnag.android;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

public class MetaData extends HashMap<String, Object> implements JsonStream.Streamable {
    private static final String FILTERED_PLACEHOLDER = "[FILTERED]";
    private static final String OBJECT_PLACEHOLDER = "[OBJECT]";

    private String[] filters;

    public MetaData() {}
    public MetaData(Map<String, Object> m) {
        super(m);
    }

    public void toStream(JsonStream writer) {
        objectToStream(this, writer);
    }

    public void addToTab(String tabName, String key, Object value) {
        Map<String, Object> tab = (Map<String, Object>)get(tabName);

        if(tab == null) {
            tab = new HashMap<String, Object>();
            put(tabName, tab);
        }

        if(value != null) {
            tab.put(key, value);
        } else {
            tab.remove(key);
        }
    }

    void setFilters(String... filters) {
        this.filters = filters;
    }

    static MetaData merge(MetaData... metaDataList) {
        return new MetaData(mergeMaps(metaDataList));
    }

    private static Map<String, Object> mergeMaps(Map<String, Object>... maps) {
        Map<String, Object> result = new HashMap<String, Object>();

        for(Map<String, Object> map : maps) {
            if(map == null) continue;

            // Get a set of all possible keys in base and overrides
            Set<String> allKeys = new HashSet<String>(result.keySet());
            allKeys.addAll(map.keySet());

            for(String key : allKeys) {
                Object baseValue = result.get(key);
                Object overridesValue = map.get(key);

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
        }

        return result;
    }

    // Write complex/nested values to a JsonStreamer
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
                        writer.value(FILTERED_PLACEHOLDER);
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
            writer.value(OBJECT_PLACEHOLDER);
        }
    }

    // Should this key be filtered
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
