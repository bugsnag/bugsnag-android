package com.bugsnag.android;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.annotation.NonNull;
import androidx.test.core.app.ApplicationProvider;

import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Date;
import java.util.Map;

final class BugsnagTestUtils {

    private BugsnagTestUtils() {
    }

    private static String streamableToString(JsonStream.Streamable streamable) throws IOException {
        StringWriter writer = new StringWriter();
        JsonStream jsonStream = new JsonStream(writer);
        streamable.toStream(jsonStream);
        return writer.toString();
    }

    static JSONObject mapToJson(Map<String, Object> map) {
        return new JSONObject(map);
    }

    static JSONObject streamableToJson(JsonStream.Streamable streamable)
        throws JSONException, IOException {
        return new JSONObject(streamableToString(streamable));
    }

    static JSONArray streamableToJsonArray(JsonStream.Streamable streamable)
        throws JSONException, IOException {
        return new JSONArray(streamableToString(streamable));
    }

    static SharedPreferences getSharedPrefs(Context context) {
        return context.getSharedPreferences("com.bugsnag.android", Context.MODE_PRIVATE);
    }

    static Client generateClient(Configuration config) {
        config.setDelivery(generateDelivery());
        return new Client(ApplicationProvider.getApplicationContext(), config);
    }

    static Client generateClient() {
        return generateClient(new Configuration("5d1ec5bd39a74caa1267142706a7fb21"));
    }

    static Session generateSession() {
        return new Session("test", new Date(), new User(), false, NoopLogger.INSTANCE);
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
        return ImmutableConfigKt.convertToImmutableConfig(config);
    }

    static SessionTracker generateSessionTracker() {
        Configuration config = generateConfiguration();
        return new SessionTracker(convert(config), config.callbackState,
                BugsnagTestUtils.generateClient(), generateSessionStore(), NoopLogger.INSTANCE);
    }

    static Connectivity generateConnectivity() {
        return new ConnectivityCompat(ApplicationProvider.getApplicationContext(), null);
    }

    static Device generateDevice() {
        DeviceBuildInfo buildInfo = DeviceBuildInfo.Companion.defaultInfo();
        return new Device(buildInfo, new String[]{}, null, null, null,
                109230923452L);
    }

    static DeviceWithState generateDeviceWithState() {
        DeviceBuildInfo buildInfo = DeviceBuildInfo.Companion.defaultInfo();
        return new DeviceWithState(buildInfo, new String[]{}, null, null, null,
                109230923452L, 22234423124L, 92340255592L, "portrait", new Date(0));
    }

    @NonNull
    static SessionStore generateSessionStore() {
        Context applicationContext = ApplicationProvider.getApplicationContext();
        return new SessionStore(applicationContext, NoopLogger.INSTANCE, null);
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
                null, null, null, null);
    }

    public static App generateApp() {
        return new App(generateImmutableConfig(), null, null, null, null);
    }
}
