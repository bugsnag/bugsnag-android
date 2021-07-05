package com.bugsnag.android;

import com.bugsnag.android.internal.ImmutableConfig;
import com.bugsnag.android.internal.ImmutableConfigKt;

import androidx.annotation.NonNull;
import androidx.test.core.app.ApplicationProvider;

import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class BugsnagTestUtils {

    private BugsnagTestUtils() {
    }

    static HashMap<String, Object> runtimeVersions = new HashMap<>();

    static {
        runtimeVersions.put("osBuild", "bulldog");
        runtimeVersions.put("androidApiLevel", 24);
    }

    private static String streamableToString(JsonStream.Streamable streamable) throws IOException {
        StringWriter writer = new StringWriter();
        JsonStream jsonStream = new JsonStream(writer);
        streamable.toStream(jsonStream);
        return writer.toString();
    }

    static JSONObject streamableToJson(JsonStream.Streamable streamable)
        throws JSONException, IOException {
        return new JSONObject(streamableToString(streamable));
    }

    static JSONArray streamableToJsonArray(JsonStream.Streamable streamable)
        throws JSONException, IOException {
        return new JSONArray(streamableToString(streamable));
    }

    static Client generateClient(Configuration config) {
        config.setDelivery(generateDelivery());
        return new Client(ApplicationProvider.getApplicationContext(), config);
    }

    static Client generateClient() {
        return generateClient(new Configuration("5d1ec5bd39a74caa1267142706a7fb21"));
    }

    static Session generateSession() {
        return new Session("test", new Date(), new User(), false,
                new Notifier(), NoopLogger.INSTANCE);
    }

    static Event generateEvent() {
        Throwable exc = new RuntimeException();
        Event event = new Event(
                exc,
                BugsnagTestUtils.generateImmutableConfig(),
                SeverityReason.newInstance(SeverityReason.REASON_HANDLED_EXCEPTION),
                NoopLogger.INSTANCE
        );
        event.setApp(generateAppWithState());
        event.setDevice(generateDeviceWithState());
        return event;
    }

    static Configuration generateConfiguration() {
        Configuration configuration = new Configuration("5d1ec5bd39a74caa1267142706a7fb21");
        configuration.setDelivery(generateDelivery());
        configuration.setLogger(NoopLogger.INSTANCE);
        return configuration;
    }

    static ImmutableConfig generateImmutableConfig() {
        return convert(generateConfiguration());
    }

    static ImmutableConfig convert(Configuration config) {
        try {
            config.setPersistenceDirectory(File.createTempFile("tmp", null));
        } catch (IOException ignored) {
            // swallow
        }
        return ImmutableConfigKt.convertToImmutableConfig(config, null, null, null);
    }

    static Device generateDevice() {
        DeviceBuildInfo buildInfo = DeviceBuildInfo.Companion.defaultInfo();
        return new Device(buildInfo, new String[]{}, null, null, null,
                109230923452L, runtimeVersions);
    }

    static DeviceWithState generateDeviceWithState() {
        DeviceBuildInfo buildInfo = DeviceBuildInfo.Companion.defaultInfo();
        return new DeviceWithState(buildInfo, null, null, null,
                109230923452L, runtimeVersions, 22234423124L, 92340255592L,
                "portrait", new Date(0));
    }

    public static Delivery generateDelivery() {
        return new Delivery() {
            @NotNull
            @Override
            public DeliveryStatus deliver(@NonNull EventPayload payload,
                                          @NonNull DeliveryParams deliveryParams) {
                return DeliveryStatus.DELIVERED;
            }

            @NonNull
            @Override
            public DeliveryStatus deliver(@NonNull Session payload,
                                          @NonNull DeliveryParams deliveryParams) {
                return DeliveryStatus.DELIVERED;
            }
        };
    }

    public static AppWithState generateAppWithState() {
        return new AppWithState(generateImmutableConfig(), null, null, null,
                null, null, null, null, null, null);
    }

    public static App generateApp() {
        return new App(generateImmutableConfig(), null, null, null, null, null);
    }

    /**
     * "Normalize" a map by changing all numeric types to their largest forms.
     * This is necessary for comparing the results of serialization/deserialization
     * operations because we have no control over what types the codec will choose,
     * and equals() takes into account the underlying type.
     *
     * @param map The map to normalize
     * @param <K> The key type
     * @param <V> The value type
     * @return The normalized map
     */
    @SuppressWarnings("unchecked")
    private static <K, V> Map<K, V> normalizedMap(Map<K, V> map) {
        Map<K, V> newMap = new HashMap<>(map.size());
        Set<Map.Entry<K, V>> set = map.entrySet();
        for (Map.Entry<K, V> entry: set) {
            K key = entry.getKey();
            K normalizedKey = (K)normalized(key);
            if (!key.equals(normalizedKey)) {
                key = normalizedKey;
            }
            newMap.put(key, (V)normalized(entry.getValue()));
        }
        return newMap;
    }

    /**
     * "Normalize" a list by changing all numeric types to their largest forms.
     * This is necessary for comparing the results of serialization/deserialization
     * operations because we have no control over what types the codec will choose,
     * and equals() takes into account the underlying type.
     *
     * @param list The list to normalize
     * @param <T> The element type
     * @return The normalized list
     */
    @SuppressWarnings("unchecked")
    private static <T> List<T> normalizedList(List<T> list) {
        List<T> newList = new ArrayList<>(list.size());
        for (T entry: list) {
            newList.add((T)normalized(entry));
        }
        return newList;
    }

    /**
     * "Normalize" an unknown value by changing all numeric types to their largest forms.
     * This is necessary for comparing the results of serialization/deserialization
     * operations because we have no control over what types the codec will choose,
     * and equals() takes into account the underlying type.
     *
     * This function normalizes integers, floats, lists, and maps and their subobjects.
     *
     * @param obj The object to normalize.
     * @return The normalized object (may be the same object passed in)
     */
    @SuppressWarnings("unchecked")
    public static Object normalized(Object obj) {
        if (obj instanceof Byte) {
            return ((Byte)obj).longValue();
        }
        if (obj instanceof Short) {
            return ((Short)obj).longValue();
        }
        if (obj instanceof Integer) {
            return ((Integer)obj).longValue();
        }
        if (obj instanceof Float) {
            return ((Float)obj).doubleValue();
        }
        if (obj instanceof Map) {
            return normalizedMap((Map<Object, Object>)obj);
        }
        if (obj instanceof List) {
            return normalizedList((List<Object>)obj);
        }
        return obj;
    }
}
