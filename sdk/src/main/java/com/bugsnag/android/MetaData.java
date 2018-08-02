package com.bugsnag.android;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
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

    @NonNull
    final Map<String, Object> store;
    final ObjectJsonStreamer jsonStreamer;

    /**
     * Create an empty MetaData object.
     */
    public MetaData() {
        this(new ConcurrentHashMap<String, Object>());
    }

    /**
     * Create a MetaData with values copied from an existing Map
     */
    public MetaData(@NonNull Map<String, Object> map) {
        store = new ConcurrentHashMap<>(map);
        jsonStreamer = new ObjectJsonStreamer();
    }

    @Override
    public void toStream(@NonNull JsonStream writer) throws IOException {
        jsonStreamer.objectToStream(store, writer);
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
        @SuppressWarnings("unchecked")
        Map<String, Object> tab = (Map<String, Object>) store.get(tabName);

        if (tab == null) {
            tab = new ConcurrentHashMap<>();
            store.put(tabName, tab);
        }

        return tab;
    }

    void setFilters(String... filters) {
        jsonStreamer.filters = filters;

        notifyBugsnagObservers(NotifyType.FILTERS);
    }

    String[] getFilters() {
        return jsonStreamer.filters;
    }

    @NonNull
    static MetaData merge(@NonNull MetaData... metaDataList) {
        List<Map<String, Object>> stores = new ArrayList<>();
        List<String> filters = new ArrayList<>();
        for (MetaData metaData : metaDataList) {
            if (metaData != null) {
                stores.add(metaData.store);

                if (metaData.jsonStreamer.filters != null) {
                    filters.addAll(Arrays.asList(metaData.jsonStreamer.filters));
                }
            }
        }

        @SuppressWarnings({"unchecked", "rawtypes"})
        MetaData newMeta = new MetaData(mergeMaps(stores.toArray(new Map[0])));
        newMeta.setFilters(filters.toArray(new String[filters.size()]));

        return newMeta;
    }

    @SafeVarargs
    @NonNull
    private static Map<String, Object> mergeMaps(@NonNull Map<String, Object>... maps) {
        Map<String, Object> result = new ConcurrentHashMap<>();

        for (Map<String, Object> map : maps) {
            if (map == null) {
                continue;
            }

            // Get a set of all possible keys in base and overrides
            Set<String> allKeys = new HashSet<>(result.keySet());
            allKeys.addAll(map.keySet());

            for (String key : allKeys) {
                Object baseValue = result.get(key);
                Object overridesValue = map.get(key);

                if (overridesValue != null) {
                    if (baseValue != null
                        && baseValue instanceof Map
                        && overridesValue instanceof Map) {
                        // Both original and overrides are Maps, go deeper
                        @SuppressWarnings("unchecked")
                        Map<String, Object> first = (Map<String, Object>) baseValue;
                        @SuppressWarnings("unchecked")
                        Map<String, Object> second = (Map<String, Object>) overridesValue;
                        result.put(key, mergeMaps(first, second));
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

    private void notifyBugsnagObservers(@NonNull NotifyType type) {
        setChanged();
        super.notifyObservers(type.getValue());
    }
}
