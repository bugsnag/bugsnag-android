package com.bugsnag.android.ndk;

import org.junit.Assert;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class BugsnagTestUtils {
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
