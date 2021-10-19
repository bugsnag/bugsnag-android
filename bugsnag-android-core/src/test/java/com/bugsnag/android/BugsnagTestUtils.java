package com.bugsnag.android;

import com.bugsnag.android.internal.ImmutableConfig;
import com.bugsnag.android.internal.ImmutableConfigKt;

import org.jetbrains.annotations.NotNull;
import org.junit.Assert;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class BugsnagTestUtils {

    static HashMap<String, Object> runtimeVersions = new HashMap<>();

    static {
        runtimeVersions.put("osBuild", "bulldog");
        runtimeVersions.put("androidApiLevel", 24);
    }

    static Configuration generateConfiguration() {
        Configuration configuration = new Configuration("5d1ec5bd39a74caa1267142706a7fb21");
        configuration.setDelivery(generateDelivery());
        configuration.setLogger(NoopLogger.INSTANCE);
        configuration.setProjectPackages(Collections.singleton("com.example.foo"));
        try {
            File dir = Files.createTempDirectory("test").toFile();
            configuration.setPersistenceDirectory(dir);
        } catch (IOException ignored) {
            // ignore IO exception
        }
        return configuration;
    }

    static ImmutableConfig generateImmutableConfig() {
        return convert(generateConfiguration());
    }

    static EventPayload generateEventPayload(ImmutableConfig config) {
        return new EventPayload(config.getApiKey(), generateEvent(), new Notifier(), config);
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

    static ImmutableConfig convert(Configuration config) {
        return ImmutableConfigKt.convertToImmutableConfig(config, null);
    }

    static DeviceBuildInfo generateDeviceBuildInfo() {
        return new DeviceBuildInfo(
                "samsung", "s7", "7.1", 24, "bulldog",
                "foo-google", "prod,build", "google", new String[]{"armeabi-v7a"}
        );
    }

    static Device generateDevice() {
        DeviceBuildInfo buildInfo = generateDeviceBuildInfo();
        return new Device(buildInfo, new String[]{}, null, null, null, 10923250000L,
                runtimeVersions);
    }

    static DeviceWithState generateDeviceWithState() {
        DeviceBuildInfo buildInfo = generateDeviceBuildInfo();
        return new DeviceWithState(buildInfo,null, null, null,
                109230923452L, runtimeVersions, 22234423124L,
                92340255592L, "portrait", new Date(0));
    }

    public static Delivery generateDelivery() {
        return new Delivery() {
            @NotNull
            @Override
            public DeliveryStatus deliver(@NotNull EventPayload payload,
                                          @NotNull DeliveryParams deliveryParams) {
                return DeliveryStatus.DELIVERED;
            }

            @NotNull
            @Override
            public DeliveryStatus deliver(@NotNull Session payload,
                                          @NotNull DeliveryParams deliveryParams) {
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
            Float value = ((Float)obj);
            if (value.doubleValue() - value.longValue() == 0) {
                return value.longValue();
            }
            return value.doubleValue();
        }
        if (obj instanceof BigInteger) {
            return ((BigInteger)obj).longValue();
        }
        if (obj instanceof BigDecimal) {
            BigDecimal value = ((BigDecimal)obj);
            if (value.doubleValue() - value.longValue() == 0) {
                return value.longValue();
            }
            return value.doubleValue();
        }
        if (obj instanceof Map) {
            return normalizedMap((Map<Object, Object>)obj);
        }
        if (obj instanceof List) {
            return normalizedList((List<Object>)obj);
        }
        return obj;
    }

    /**
     * Assert equality on normalized deep copies of list & map containers so that different
     * sized numeric fields containing the same value will be considered equal.
     *
     * @param expected The expected value
     * @param observed The observed value
     */
    public static void assertNormalizedEquals(Object expected, Object observed) {
        Assert.assertEquals(normalized(expected), normalized(observed));
    }
}
