package com.bugsnag.android;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Deserializes a stacktrace from the 'nativeStack' property supplied by React Native.
 */
class NativeStackDeserializer implements MapDeserializer<List<Stackframe>> {

    private final Collection<String> projectPackages;

    NativeStackDeserializer(Collection<String> projectPackages) {
        this.projectPackages = projectPackages;
    }

    /**
     * Constructs a native stacktrace from the given payload. This assumes that 'nativeStack'
     * contains a list of stackframes containing the methodName, class, file, and lineNumber.
     *
     * @param map the JSON payload passed from the JS layer
     * @return a representation of a native stacktrace
     */
    @Override
    public List<Stackframe> deserialize(Map<String, Object> map) {
        List<Map<String, Object>> nativeStack = MapUtils.getOrThrow(map, "nativeStack");
        List<Stackframe> frames = new ArrayList<>();

        for (Map<String, Object> frame : nativeStack) {
            frames.add(deserializeStackframe(frame, projectPackages));
        }
        return new Stacktrace(frames).getTrace();
    }

    private Stackframe deserializeStackframe(Map<String, Object> map,
                                             Collection<String> projectPackages) {
        String methodName = MapUtils.getOrNull(map, "methodName");
        if (methodName == null) {
            methodName = "";
        }

        String clz = MapUtils.getOrNull(map, "class");
        String method = String.format("%s.%s", clz, methodName);

        // RN <0.63.2 doesn't add class, gracefully fallback by only reporting
        // method name. see https://github.com/facebook/react-native/pull/25014
        if (clz == null) {
            clz = "";
            method = methodName;
        }
        Stackframe stackframe = new Stackframe(
                method,
                MapUtils.<String>getOrNull(map, "file"),
                MapUtils.<Integer>getOrNull(map, "lineNumber"),
                Stacktrace.Companion.inProject(clz, projectPackages)
        );
        stackframe.setType(ErrorType.ANDROID);
        return stackframe;
    }
}
