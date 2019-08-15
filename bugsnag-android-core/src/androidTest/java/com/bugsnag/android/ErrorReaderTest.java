package com.bugsnag.android;

import static com.bugsnag.android.BugsnagTestUtils.streamableToJson;
import static com.bugsnag.android.BugsnagTestUtils.streamableToJsonArray;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import androidx.test.filters.SmallTest;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

@SmallTest
public class ErrorReaderTest {

    private static Error error = null;

    /** Constructs an Error from a JSON file */
    @BeforeClass
    public static void setUp() throws IOException {
        ClassLoader classLoader = ErrorReaderTest.class.getClassLoader();
        InputStream input = classLoader.getResourceAsStream("error.json");
        File fixtureFile = File.createTempFile("error", ".json");
        OutputStream output = new FileOutputStream(fixtureFile);
        try {
            byte[] buffer = new byte[1024];
            int read;
            while ((read = input.read(buffer)) != -1) {
                output.write(buffer, 0, read);
            }
            output.flush();
        } finally {
            output.close();
        }
        error = ErrorReader.readError(new Configuration("key"), fixtureFile);
    }

    @Test
    public void testReadError() throws IOException {
        assertNotNull(error);
    }

    @Test(expected = IOException.class)
    public void testReadPartialFileThrows() throws IOException {
        File fixtureFile = null;
        try {
            fixtureFile = File.createTempFile("error", ".json");
            FileOutputStream output = new FileOutputStream(fixtureFile);
            output.write("{".getBytes());
        } catch (Exception ex) {
            assertTrue("Failed to configure test", false);
        }
        ErrorReader.readError(new Configuration("api-key"), fixtureFile);
    }

    @Test(expected = IOException.class)
    public void testReadEmptyErrorFileThrows() throws IOException {
        File fixtureFile = null;
        try {
            fixtureFile = File.createTempFile("error", ".json");
            FileOutputStream output = new FileOutputStream(fixtureFile);
            output.write("{}".getBytes());
        } catch (Exception ex) {
            assertTrue("Failed to configure test", false);
        }
        ErrorReader.readError(new Configuration("api-key"), fixtureFile);
    }

    @Test
    public void testReadErrorSeverity() throws JSONException {
        assertEquals(Severity.WARNING, error.getSeverity());
    }

    @Test
    public void testReadErrorSession() throws Exception {
        Session session = error.getSession();
        assertNotNull(session);
        assertEquals("225bcada-e0c8-15a0-0bba-0e3c7f43c13f", session.getId());
        assertEquals(2, session.getHandledCount());
        assertEquals(1, session.getUnhandledCount());

        TimeZone tz = TimeZone.getTimeZone("UTC");
        DateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        format.setTimeZone(tz);
        assertEquals("2018-10-19T18:56:13Z", format.format(session.getStartedAt()));

        User user = session.getUser();
        assertEquals("Rasputin", user.getName());
        assertEquals("ras@example.com", user.getEmail());
        assertEquals("9659cc1f-fec8-47a3-8ad2-0e3c7e84c24d", user.getId());
    }

    @Test
    public void testReadErrorAppData() throws JSONException {
        Map<String, Object> appData = error.getAppData();
        assertNotNull(appData);
        assertEquals(appData.get("version"), "1.1.14");
        assertEquals(appData.get("id"), "com.bugsnag.android.mazerunner");
        assertEquals(appData.get("type"), "android");
        assertEquals(appData.get("releaseStage"), "beta4");
        assertEquals(appData.get("versionCode"), 34);
        assertEquals(appData.get("buildUUID"), "2338403b-4ca1-40f5-bdee-3b82c2cb305a");
        assertEquals(appData.get("duration"), 169);
        assertEquals(appData.get("durationInForeground"), 0);
        assertEquals(true, appData.get("inForeground"));
    }

    @Test
    public void testReadErrorDeviceData() throws JSONException {
        Map<String, Object> data = error.getDeviceData();
        assertNotNull(data);
        assertEquals("android", data.get("osName"));
        assertEquals("9659cc1f-fec8-47a3-8ad2-0e3c7e84c24d", data.get("id"));
        assertEquals("5.0.2", data.get("osVersion"));
        assertEquals("Hudan", data.get("manufacturer"));
        assertEquals("sdk_phone_armv7", data.get("model"));
        assertEquals("portrait", data.get("orientation"));
        assertEquals(134217728, data.get("totalMemory"));

        @SuppressWarnings("unchecked")
        List<String> cpuAbi = (List<String>) data.get("cpuAbi");
        assertEquals("armeabi-v7a", cpuAbi.get(0));
        assertEquals("armeabi", cpuAbi.get(1));
    }

    @Test
    public void testReadErrorHandledState() throws JSONException {
        HandledState state = error.getHandledState();
        assertNotNull(state);
        assertTrue(state.isUnhandled());
        assertEquals("signal", state.calculateSeverityReasonType());
    }

    @Test
    public void testReadErrorMetaData() throws JSONException {
        MetaData metadata = error.getMetaData();
        assertNotNull(metadata);
        assertEquals("com.bugsnag.android.mazerunner", metadata.getTab("app").get("packageName"));
        assertEquals("1.1.14", metadata.getTab("app").get("versionName"));
        assertEquals("MainActivity", metadata.getTab("app").get("activeScreen"));
        assertEquals("MazeRunner", metadata.getTab("app").get("name"));
        assertEquals(false, metadata.getTab("app").get("lowMemory"));
        assertEquals(false, metadata.getTab("customer").get("accountee"));
        assertEquals("Acme Co", metadata.getTab("customer").get("name"));
        assertEquals(23423, metadata.getTab("customer").get("id"));
        assertEquals("sdk_phone_armv7-eng 5.0.2 LSY66K 3079185 test-keys",
                     metadata.getTab("device").get("osBuild"));
        assertEquals(21, metadata.getTab("device").get("apiLevel"));
        assertEquals("O-Matic", metadata.getTab("device").get("brand"));
        assertEquals(false, metadata.getTab("device").get("emulator"));
        assertEquals(true, metadata.getTab("device").get("jailbroken"));
        assertEquals("en_US", metadata.getTab("device").get("locale"));
        assertEquals("allowed", metadata.getTab("device").get("locationStatus"));
        assertEquals("cellular", metadata.getTab("device").get("networkAccess"));
        assertEquals(160, metadata.getTab("device").get("dpi"));
        assertEquals(1, metadata.getTab("device").get("screenDensity"));
        assertEquals("480x320", metadata.getTab("device").get("screenResolution"));
        assertEquals("2018-10-19T18:56:13Z", metadata.getTab("device").get("time"));
    }

    @Test
    public void testReadErrorUser() throws JSONException {
        User user = error.getUser();
        assertNotNull(user);
        assertEquals("Rasputin", user.getName());
        assertEquals("ras@example.com", user.getEmail());
        assertEquals("9659cc1f-fec8-47a3-8ad2-0e3c7e84c24d", user.getId());
    }

    @Test
    public void testReadErrorExceptionClass() throws JSONException, IOException {
        assertEquals("SIGSEGV",
                     streamableToJsonArray(error.getExceptions())
                                                .getJSONObject(0).getString("errorClass"));
    }

    @Test
    public void testReadErrorExceptionMessage() throws JSONException, IOException {
        assertEquals("Segmentation violation (invalid memory reference)",
                     streamableToJsonArray(error.getExceptions())
                                                .getJSONObject(0).getString("message"));
    }

    @Test
    public void testReadErrorExceptionType() throws JSONException, IOException {
        assertEquals("c", streamableToJsonArray(error.getExceptions())
                            .getJSONObject(0).getString("type"));
    }

    @Test
    public void testReadErrorExceptionStacktrace() throws JSONException, IOException {
        JSONArray stacktrace = streamableToJsonArray(error.getExceptions())
                                    .getJSONObject(0)
                                    .getJSONArray("stacktrace");
        assertEquals(3, stacktrace.length());

        JSONObject frame0 = stacktrace.getJSONObject(0);
        assertEquals("_ZNK3art6Thread13DecodeJObjectEP8_jobject", frame0.getString("method"));
        assertEquals("libmuraccis.so", frame0.getString("file"));
        assertEquals(2241790, frame0.getInt("lineNumber"));

        JSONObject frame1 = stacktrace.getJSONObject(1);
        assertEquals("java.lang.Daemons$ReferenceQueueDaemon.run", frame1.getString("method"));
        assertEquals("Daemons.java", frame1.getString("file"));
        assertEquals(150, frame1.getInt("lineNumber"));

        JSONObject frame2 = stacktrace.getJSONObject(2);
        assertEquals("java.lang.Thread.run", frame2.getString("method"));
        assertEquals("Thread.java", frame2.getString("file"));
        assertEquals(761, frame2.getInt("lineNumber"));
    }

    @Test
    public void testReadErrorGroupingHash() throws JSONException {
        assertEquals("400-b", error.getGroupingHash());
    }

    @Test
    public void testReadErrorContext() throws JSONException {
        assertEquals("MainActivity", error.getContext());
    }

    @Test
    public void testReadErrorBreadcrumbs() throws JSONException, IOException {
        JSONArray breadcrumbs = streamableToJson(error).getJSONArray("breadcrumbs");
        assertNotNull(breadcrumbs);
        assertEquals(3, breadcrumbs.length());

        JSONObject crumb0 = breadcrumbs.getJSONObject(0);
        assertEquals("MainActivity", crumb0.getString("name"));
        assertEquals("navigation", crumb0.getString("type"));
        assertEquals("2018-10-19T18:56:13Z", crumb0.getString("timestamp"));
        JSONObject crumb0metadata = crumb0.getJSONObject("metaData");
        assertEquals(1, crumb0metadata.length());
        assertEquals("onStart()", crumb0metadata.getString("ActivityLifecycle"));

        JSONObject crumb1 = breadcrumbs.getJSONObject(1);
        assertEquals("MainActivity", crumb1.getString("name"));
        assertEquals("navigation", crumb1.getString("type"));
        assertEquals("2018-10-19T18:56:13Z", crumb1.getString("timestamp"));
        JSONObject crumb1metadata = crumb1.getJSONObject("metaData");
        assertEquals(1, crumb1metadata.length());
        assertEquals("onResume()", crumb1metadata.getString("ActivityLifecycle"));

        JSONObject crumb2 = breadcrumbs.getJSONObject(2);
        assertEquals("RINGER_MODE_CHANGED", crumb2.getString("name"));
        assertEquals("state", crumb2.getString("type"));
        assertEquals("2018-10-19T18:56:13Z", crumb2.getString("timestamp"));
        JSONObject crumb2metadata = crumb2.getJSONObject("metaData");
        assertEquals(2, crumb2metadata.length());
        assertEquals("RINGER_MODE_CHANGED: 2", crumb2metadata.getString("Extra"));
        assertEquals("android.media.RINGER_MODE_CHANGED",
                     crumb2metadata.getString("Intent Action"));
    }

    @Test
    public void testReadErrorThreadState() throws JSONException, IOException {
        JSONArray threads = streamableToJson(error).getJSONArray("threads");
        assertNotNull(threads);
        assertEquals(7, threads.length());
        for (int i = 0; i < threads.length(); i++) {
            JSONObject thread = threads.getJSONObject(i);
            assertNotNull(thread.getString("name"));
            assertEquals("android", thread.getString("type"));
            assertNotNull(thread.getInt("id"));
            JSONArray stacktrace = thread.getJSONArray("stacktrace");
            assertFalse(stacktrace.length() == 0);
            for (int j = 0; j < stacktrace.length(); j++) {
                JSONObject frame = stacktrace.getJSONObject(j);
                assertNotNull(frame.getString("method"));
                assertNotNull(frame.getString("file"));
                assertNotEquals(0, frame.getInt("lineNumber"));
            }
        }
    }
}
