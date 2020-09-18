package com.bugsnag.android;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Deserializes an error from the 'nativeStack' property supplied by React Native
 *
 * This requires the original JS error, whose error message/class is used by the native error.
 */
class NativeErrorDeserializer implements MapDeserializer<Error> {

    private final Error jsError;
    private final Logger logger;
    private final Collection<String> projectPackages;

    NativeErrorDeserializer(Error jsError, Collection<String> projectPackages, Logger logger) {
        this.jsError = jsError;
        this.projectPackages = projectPackages;
        this.logger = logger;
    }

    /**
     * Constructs a native error from the given payload. This assumes that 'nativeStack' contains
     * a list of stackframes containing the methodName, class, file, and lineNumber.
     *
     * @param map the JSON payload passed from the JS layer
     * @return a representation of a native error
     */
    @Override
    public Error deserialize(Map<String, Object> map) {
        List<Map<String, Object>> nativeStack = MapUtils.getOrThrow(map, "nativeStack");
        List<Stackframe> frames = new ArrayList<>();

        for (Map<String, Object> frame : nativeStack) {
            frames.add(deserializeStackframe(frame, projectPackages));
        }

        Stacktrace trace = new Stacktrace(frames, logger);
        ErrorInternal impl = new ErrorInternal(
                jsError.getErrorClass(),
                jsError.getErrorMessage(),
                trace,
                ErrorType.ANDROID
        );
        return new Error(impl, logger);
    }

    private Stackframe deserializeStackframe(Map<String, Object> map,
                                             Collection<String> projectPackages) {
        String methodName = MapUtils.getOrNull(map, "methodName");
        String clz = MapUtils.getOrNull(map, "class");

        if (methodName == null) {
            methodName = "";
        }
        if (clz == null) {
            clz = "";
        }
        return new Stackframe(
                String.format("%s.%s", clz, methodName),
                MapUtils.<String>getOrNull(map, "file"),
                MapUtils.<Integer>getOrNull(map, "lineNumber"),
                Stacktrace.Companion.inProject(clz, projectPackages)
        );
    }
}
