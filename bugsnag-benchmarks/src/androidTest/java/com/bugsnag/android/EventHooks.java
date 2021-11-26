package com.bugsnag.android;

import static com.bugsnag.android.ClientHooksKt.generateConfig;

import com.bugsnag.android.internal.ImmutableConfig;
import com.bugsnag.android.internal.ImmutableConfigKt;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;

public class EventHooks {

    static HashMap<String, Object> runtimeVersions = new HashMap<>();

    static {
        runtimeVersions.put("osBuild", "bulldog");
        runtimeVersions.put("androidApiLevel", "24");
    }

    public static EventPayload generateEvent() {
        Throwable exc = new RuntimeException();
        ImmutableConfig cfg = convert(generateConfig());
        Event event = new Event(
                exc,
                cfg,
                SeverityReason.newInstance(SeverityReason.REASON_HANDLED_EXCEPTION),
                NoopLogger.INSTANCE
        );
        event.setApp(generateAppWithState(cfg));
        event.setDevice(generateDeviceWithState());
        return new EventPayload("api-key", event, null, new Notifier(), cfg);
    }

    static ImmutableConfig convert(Configuration config) {
        try {
            config.setPersistenceDirectory(File.createTempFile("tmp", null));
        } catch (IOException ignored) {
            // swallow
        }
        return ImmutableConfigKt.convertToImmutableConfig(config, null);
    }

    static AppWithState generateAppWithState(ImmutableConfig cfg) {
        return new AppWithState(cfg,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null);
    }

    static DeviceWithState generateDeviceWithState() {
        DeviceBuildInfo buildInfo = DeviceBuildInfo.Companion.defaultInfo();
        return new DeviceWithState(buildInfo,
                null,
                null,
                null,
                109230923452L,
                runtimeVersions,
                22234423124L,
                92340255592L,
                "portrait",
                new Date(0));
    }
}
