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
        // TODO merging of values

        if (value == null) {
            clearMetadata(section, key);
        } else {
            Map<String, Object> tab = getOrAddSection(section);

            if (key == null) {
                store.put(section, value);
            } else {
                tab.put(key, value);
            }

            setChanged();
            notifyObservers(new NativeInterface.Message(
                    NativeInterface.MessageType.ADD_METADATA,
                    Arrays.asList(section, key, value)));
        }
    }

    @Override
    public void clearMetadata(@NotNull String section, @Nullable String key) {
        setChanged();

        if (key == null) {
            store.remove(section);
            notifyObservers(new NativeInterface.Message(
                    NativeInterface.MessageType.CLEAR_METADATA_TAB, section));
        } else {
            Object tab = store.get(section);

            if (tab instanceof Map) {
                ((Map) tab).remove(key);
            }
            notifyObservers(new NativeInterface.Message(
                    NativeInterface.MessageType.REMOVE_METADATA, Arrays.asList(section, key)));
        }
    }

    @SuppressWarnings("unchecked")
    @Nullable
    @Override
    public Object getMetadata(@NotNull String section, @Nullable String key) {
        Object tab = store.get(section);

        if (!(tab instanceof Map) || key == null) {
            return tab;
        } else {
            Map<String, Object> map = (Map<String, Object>) tab;
            return map.get(key);
        }
    }

    @SuppressWarnings("unchecked")
    @NotNull
    private Map<String, Object> getOrAddSection(@NonNull String section) {
        Object tab = store.get(section);

        if (!(tab instanceof Map)) {
            tab = new ConcurrentHashMap<>();
            store.put(section, tab);
        }

        return (Map<String, Object>) tab;
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
