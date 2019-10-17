package com.bugsnag.android;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
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
public class MetaData extends Observable implements JsonStream.Streamable, MetaDataAware {

    @NonNull
    final Map<String, Object> store;
    final ObjectJsonStreamer jsonStreamer;

    MetaData() {
        this(new ConcurrentHashMap<String, Object>());
    }

    MetaData(@NonNull Map<String, Object> map) {
        store = new ConcurrentHashMap<>(map);
        jsonStreamer = new ObjectJsonStreamer();
    }

    @Override
    public void toStream(@NonNull JsonStream writer) throws IOException {
        jsonStreamer.objectToStream(store, writer);
    }

    @Override
    public void addMetadata(@NonNull String section, @Nullable String key, @Nullable Object value) {
        Map<String, Object> tab = getTab(section);
        setChanged();
        if (value != null) {
            tab.put(key, value);
            notifyObservers(new NativeInterface.Message(
                    NativeInterface.MessageType.ADD_METADATA,
                    Arrays.asList(section, key, value)));
        } else {
            tab.remove(key);
            notifyObservers(new NativeInterface.Message(
                    NativeInterface.MessageType.REMOVE_METADATA,
                    Arrays.asList(section, key)));
        }
    }

    @Override
    public void clearMetadata(@NotNull String section, @Nullable String key) {
        store.remove(section);
        setChanged();
        notifyObservers(new NativeInterface.Message(
                NativeInterface.MessageType.CLEAR_METADATA_TAB, section));
    }

    @Nullable
    @Override
    public Object getMetadata(@NotNull String section, @Nullable String key) {
        return null;
    }

    @NonNull
    private Map<String, Object> getTab(String tabName) {
        @SuppressWarnings("unchecked")
        Map<String, Object> tab = (Map<String, Object>) store.get(tabName);

        if (tab == null) {
            tab = new ConcurrentHashMap<>();
            store.put(tabName, tab);
        }

        return tab;
    }

    void setFilters(Collection<String> filters) {
        Collection<String> data = new HashSet<>(filters);
        jsonStreamer.filters.clear();
        jsonStreamer.filters.addAll(data);
    }

    Set<String> getFilters() {
        return jsonStreamer.filters;
    }

    @NonNull
    static MetaData merge(@NonNull MetaData... metaDataList) {
        List<Map<String, Object>> stores = new ArrayList<>();
        Collection<String> filters = new HashSet<>();
        for (MetaData metaData : metaDataList) {
            if (metaData != null) {
                stores.add(metaData.store);

                if (metaData.jsonStreamer.filters != null) {
                    filters.addAll(metaData.jsonStreamer.filters);
                }
            }
        }

        @SuppressWarnings({"unchecked", "rawtypes"})
        MetaData newMeta = new MetaData(mergeMaps(stores.toArray(new Map[0])));
        newMeta.setFilters(filters);
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
                    if (baseValue instanceof Map && overridesValue instanceof Map) {
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
                    if (baseValue != null) { // No collision, just use base value
                        result.put(key, baseValue);
                    }
                }
            }
        }

        return result;
    }
}
