package com.bugsnag.android;

import android.support.test.InstrumentationRegistry;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import org.json.JSONObject;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.StringWriter;

import static com.bugsnag.android.ErrorStore.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class ErrorStoreTest  {

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

        Error error = new Error(config, new RuntimeException());
        errorStore.write(error);

        files = errorStorageDir.listFiles();
        assertEquals(baseline + 1, files.length);
        File file = files[0];
        checkFirstErrorReportFile(file);
    }

    @Test
    public void testIsLaunchCrashReport() throws Exception {
        String[] valid = {"1504255147933_startupcrash.json"};
        String[] invalid = {"", ".json", "abcdeAO.json", "!@£)(%)(.txt", "1504255147933.txt", "1504255147933.json"};

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

    static void checkFirstErrorReportFile(File errorFile) throws Exception {
        // ensure that the file isn't empty
        assertFalse(errorFile.length() <= 0);

        // ensure the file can be serialised into JSON report
        Report report = new Report("abc", errorFile);
        StringWriter stringWriter = new StringWriter();
        report.toStream(new JsonStream(stringWriter));
        JSONObject payload = new JSONObject(stringWriter.toString());
        assertNotNull(payload);
        assertEquals(1, payload.getJSONArray("events").length());
    }

}
