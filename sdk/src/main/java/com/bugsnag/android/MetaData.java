package com.bugsnag.android;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.io.IOException;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A container for additional diagnostic information you'd like to send with
 * every error report.
 * <p>
 * Diagnostic information is presented on your Bugsnag dashboard in tabs.
 */
public class MetaData extends Observable implements JsonStream.Streamable {
    private static final String FILTERED_PLACEHOLDER = "[FILTERED]";
    private static final String OBJECT_PLACEHOLDER = "[OBJECT]";

    private String[] filters;
    @NonNull
    final Map<String, Object> store;

    /**
     * Create an empty MetaData object.
     */
    public MetaData() {
        store = new ConcurrentHashMap<>();
    }

    /**
     * Create a MetaData with values copied from an existing Map
     */
    public MetaData(@NonNull Map<String, Object> m) {
        store = new ConcurrentHashMap<>(m);
    }

    @Override
    public void toStream(@NonNull JsonStream writer) throws IOException {
        objectToStream(store, writer);
    }

    /**
     * Add diagnostic information to a tab of this MetaData.
     * <p>
     * For example:
     * <p>
     * metaData.addToTab("account", "name", "Acme Co.");
     * metaData.addToTab("account", "payingCustomer", true);
     *
     * @param tabName the dashboard tab to add diagnostic data to
     * @param key     the name of the diagnostic information
     * @param value   the contents of the diagnostic information
     */
    public void addToTab(String tabName, String key, Object value) {
        addToTab(tabName, key, value, true);
    }

    /**
     * Add diagnostic information to a tab of this MetaData.
     * <p>
     * For example:
     * <p>
     * metaData.addToTab("account", "name", "Acme Co.");
     * metaData.addToTab("account", "payingCustomer", true);
     *
     * @param tabName the dashboard tab to add diagnostic data to
     * @param key     the name of the diagnostic information
     * @param value   the contents of the diagnostic information
     * @param notify  whether or not to notify any NDK observers about this change
     */
    void addToTab(String tabName, String key, @Nullable Object value, boolean notify) {
        Map<String, Object> tab = getTab(tabName);

        if (value != null) {
            tab.put(key, value);
        } else {
            tab.remove(key);
        }

        notifyBugsnagObservers(NotifyType.META);
    }

    /**
     * Remove a tab of diagnostic information from this MetaData.
     *
     * @param tabName the dashboard tab to remove diagnostic data from
     */
    public void clearTab(String tabName) {
        store.remove(tabName);

        notifyBugsnagObservers(NotifyType.META);
    }

    @NonNull
    Map<String, Object> getTab(String tabName) {
        Map<String, Object> tab = (Map<String, Object>) store.get(tabName);

        if (tab == null) {
            tab = new ConcurrentHashMap<>();
            store.put(tabName, tab);
        }

        return tab;
    }

    void setFilters(String... filters) {
        this.filters = filters;

        notifyBugsnagObservers(NotifyType.FILTERS);
    }

    @NonNull
    static MetaData merge(@NonNull MetaData... metaDataList) {
        List<Map<String, Object>> stores = new ArrayList<>();
        List<String> filters = new ArrayList<>();
        for (MetaData metaData : metaDataList) {
            if (metaData != null) {
                stores.add(metaData.store);

                if (metaData.filters != null) {
                    filters.addAll(Arrays.asList(metaData.filters));
                }
            }
        }

        MetaData newMeta = new MetaData(mergeMaps(stores.toArray(new Map[0])));
        newMeta.filters = filters.toArray(new String[filters.size()]);

        return newMeta;
    }

    @NonNull
    private static Map<String, Object> mergeMaps(@NonNull Map<String, Object>... maps) {
        Map<String, Object> result = new ConcurrentHashMap<>();

        for (Map<String, Object> map : maps) {
            if (map == null) continue;

            // Get a set of all possible keys in base and overrides
            Set<String> allKeys = new HashSet<>(result.keySet());
            allKeys.addAll(map.keySet());

            for (String key : allKeys) {
                Object baseValue = result.get(key);
                Object overridesValue = map.get(key);

                if (overridesValue != null) {
                    if (baseValue != null && baseValue instanceof Map && overridesValue instanceof Map) {
                        // Both original and overrides are Maps, go deeper
                        result.put(key, mergeMaps((Map<String, Object>) baseValue, (Map<String, Object>) overridesValue));
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
    private void objectToStream(@Nullable Object obj, @NonNull JsonStream writer) throws IOException {
        if (obj == null) {
            writer.nullValue();
        } else if (obj instanceof String) {
            writer.value((String) obj);
        } else if (obj instanceof Number) {
            writer.value((Number) obj);
        } else if (obj instanceof Boolean) {
            writer.value((Boolean) obj);
        } else if (obj instanceof Map) {
            // Map objects
            writer.beginObject();
            for (Object o : ((Map) obj).entrySet()) {
                Map.Entry entry = (Map.Entry) o;
                Object keyObj = entry.getKey();
                if (keyObj instanceof String) {
                    String key = (String) keyObj;
                    writer.name(key);
                    if (shouldFilter(key)) {
                        writer.value(FILTERED_PLACEHOLDER);
                    } else {
                        objectToStream(entry.getValue(), writer);
                    }
                }
            }
            writer.endObject();
        } else if (obj instanceof Collection) {
            // Collection objects (Lists, Sets etc)
            writer.beginArray();
            for (Object entry : (Collection) obj) {
                objectToStream(entry, writer);
            }
            writer.endArray();
        } else if (obj.getClass().isArray()) {
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
    private boolean shouldFilter(@Nullable String key) {
        if (filters == null || key == null) return false;

        for (String filter : filters) {
            if (key.contains(filter)) {
                return true;
            }
        }

        return false;
    }

    private void notifyBugsnagObservers(@NonNull NotifyType type) {
        setChanged();
        super.notifyObservers(type.getValue());
    }
}
