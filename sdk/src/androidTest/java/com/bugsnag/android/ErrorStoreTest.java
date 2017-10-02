package com.bugsnag.android;

import android.support.annotation.NonNull;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;

import static com.bugsnag.android.ErrorStore.ERROR_REPORT_COMPARATOR;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class ErrorStoreTest {

    private ErrorStore errorStore;
    private Configuration config;
    private File errorStorageDir;

    @Before
    public void setUp() throws Exception {
        Client client = new Client(InstrumentationRegistry.getContext(), "api-key");
        config = client.config;
        errorStore = client.errorStore;
        Assert.assertNotNull(errorStore.path);
        errorStorageDir = new File(errorStore.path);
        FileUtils.clearFilesInDir(errorStorageDir);
    }

    @After
    public void tearDown() throws Exception {
        FileUtils.clearFilesInDir(errorStorageDir);
    }

    @Test
    public void testWrite() throws Exception {
        File[] files = errorStorageDir.listFiles();
        int baseline = files.length; // record baseline number of files

        Error error = new Error.Builder(config, new RuntimeException()).build();
        errorStore.write(error);

        files = errorStorageDir.listFiles();
        assertEquals(baseline + 1, files.length);
        File file = files[0];
        checkFileMatchesErrorReport(file, error);
    }

    @Test
    public void testIsLaunchCrashReport() throws Exception {
        String[] valid = {"1504255147933_startupcrash.json"};
        String[] invalid = {"", ".json", "abcdeAO.json", "!@£)(%)(", "1504255147933.txt", "1504255147933.json"};

        for (String s : valid) {
            assertTrue(errorStore.isLaunchCrashReport(new File(s)));
        }
        for (String s : invalid) {
            assertFalse(errorStore.isLaunchCrashReport(new File(s)));
        }
    }

    @Test
    public void testComparator() throws Exception {
        String first = "1504255147933.json";
        String second = "1505000000000.json";
        String startup = "1504500000000_startupcrash.json";

        // handle defaults
        assertEquals(0, ERROR_REPORT_COMPARATOR.compare(null, null));
        assertEquals(-1, ERROR_REPORT_COMPARATOR.compare(new File(""), null));
        assertEquals(1, ERROR_REPORT_COMPARATOR.compare(null, new File("")));

        // same value should always be 0
        assertEquals(0, ERROR_REPORT_COMPARATOR.compare(new File(first), new File(first)));
        assertEquals(0, ERROR_REPORT_COMPARATOR.compare(new File(startup), new File(startup)));

        // first is before second
        assertTrue(ERROR_REPORT_COMPARATOR.compare(new File(first), new File(second)) < 0);
        assertTrue(ERROR_REPORT_COMPARATOR.compare(new File(second), new File(first)) > 0);

        // startup is handled correctly
        assertTrue(ERROR_REPORT_COMPARATOR.compare(new File(first), new File(startup)) < 0);
        assertTrue(ERROR_REPORT_COMPARATOR.compare(new File(second), new File(startup)) > 0);
    }


    /**
     * Ensures that the file can be serialised back into a JSON report, and contains the same info
     * as the original
     */
    private static void checkFileMatchesErrorReport(File file, Error error) throws Exception {
        // ensure that the file isn't empty
        assertFalse(file.length() <= 0);

        // ensure the file can be serialised into JSON report
        JSONObject memory = getJsonObjectFromReport(new Report("abc", file));
        JSONObject disk = getJsonObjectFromReport(new Report("abc", error));

        // validate info
        validateReportPayload(memory);
        validateReportPayload(disk);
    }

    static void validateReportPayload(JSONObject payload) throws JSONException {
        assertNotNull(payload);
        assertEquals(3, payload.length());

        JSONArray events = payload.getJSONArray("events");
        assertNotNull(events);
        assertEquals(1, events.length());

        JSONObject error = events.getJSONObject(0);
        assertNotNull(error);

        JSONObject metaData = error.getJSONObject("metaData");
        assertNotNull(metaData);
    }

    @NonNull
    static JSONObject getJsonObjectFromReport(Report report) throws IOException, JSONException {
        StringWriter stringWriter = new StringWriter();
        report.toStream(new JsonStream(stringWriter));
        return new JSONObject(stringWriter.toString());
    }

}
