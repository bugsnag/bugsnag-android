package com.bugsnag.android;

import static com.bugsnag.android.ErrorStore.ERROR_REPORT_COMPARATOR;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.support.annotation.NonNull;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Collections;
import java.util.List;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class ErrorStoreTest {

    private ErrorStore errorStore;
    private Configuration config;
    private File errorStorageDir;

    /**
     * Generates a client and ensures that its errorStore has 0 files persisted
     *
     * @throws Exception if initialisation failed
     */
    @Before
    public void setUp() throws Exception {
        config = new Configuration("api-key");
        errorStore = new ErrorStore(config, InstrumentationRegistry.getContext(), null);
        assertNotNull(errorStore.storeDirectory);
        errorStorageDir = new File(errorStore.storeDirectory);
        FileUtils.clearFilesInDir(errorStorageDir);
    }

    /**
     * Removes any files from the errorStore generated during testing
     *
     * @throws Exception if removing files failed
     */
    @After
    public void tearDown() throws Exception {
        FileUtils.clearFilesInDir(errorStorageDir);
    }

    @NonNull
    private Error writeErrorToStore() {
        Error error = new Error.Builder(config, new RuntimeException(),
            BugsnagTestUtils.generateSessionTracker(), Thread.currentThread(), false).build();
        errorStore.write(error);
        return error;
    }

    @Test
    public void testIsLaunchCrashReport() throws Exception {
        String[] valid = {"1504255147933__30b7e350-dcd1-4032-969e-98d30be62bbc_startupcrash.json"};
        String[] invalid = {"", ".json", "abcdeAO.json", "!@£)(%)(",
            "1504255147933.txt", "1504255147933.json"};

        for (String s : valid) {
            assertTrue(errorStore.isLaunchCrashReport(new File(s)));
        }
        for (String s : invalid) {
            assertFalse(errorStore.isLaunchCrashReport(new File(s)));
        }
    }

    @Test
    public void testComparator() throws Exception {
        final String first = "1504255147933_683c6b92-b325-4987-80ad-77086509ca1e.json";
        final String second = "1505000000000_683c6b92-b325-4987-80ad-77086509ca1e.json";
        final String startup = "1504500000000_683c6b92-b325-"
            + "4987-80ad-77086509ca1e_startupcrash.json";

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

    @Test
    public void isStartupCrash() throws Exception {
        assertTrue(errorStore.isStartupCrash(0));

        config.setLaunchCrashThresholdMs(0);
        assertFalse(errorStore.isStartupCrash(0));

        config.setLaunchCrashThresholdMs(10000);
        assertTrue(errorStore.isStartupCrash(5345));
        assertTrue(errorStore.isStartupCrash(9999));
        assertFalse(errorStore.isStartupCrash(10000));
    }

    @Test
    public void testCancelQueuedFiles() {
        assertEquals(0, errorStore.queuedFiles.size());
        writeErrorToStore();
        assertEquals(0, errorStore.queuedFiles.size());

        List<File> storedFiles = errorStore.findStoredFiles();
        assertEquals(1, storedFiles.size());
        errorStore.cancelQueuedFiles(null);
        assertEquals(1, errorStore.queuedFiles.size());

        errorStore.cancelQueuedFiles(Collections.<File>emptyList());
        assertEquals(1, errorStore.queuedFiles.size());

        errorStore.cancelQueuedFiles(storedFiles);
        assertEquals(0, errorStore.queuedFiles.size());
    }

    @Test
    public void testDeleteQueuedFiles() {
        assertEquals(0, errorStore.findStoredFiles().size());

        writeErrorToStore();
        List<File> storedFiles = errorStore.findStoredFiles();
        assertEquals(1, storedFiles.size());

        errorStore.deleteStoredFiles(null);
        assertEquals(1, errorStore.queuedFiles.size());

        errorStore.deleteStoredFiles(Collections.<File>emptyList());
        assertEquals(1, errorStore.queuedFiles.size());


        errorStore.deleteStoredFiles(storedFiles);
        assertEquals(0, errorStore.findStoredFiles().size());
        assertEquals(0, errorStore.queuedFiles.size());
        assertEquals(0, new File(errorStore.storeDirectory).listFiles().length);
    }

    @Test
    public void testFileQueueDuplication() {
        writeErrorToStore();
        List<File> ogFiles = errorStore.findStoredFiles();
        assertEquals(1, ogFiles.size());

        List<File> storedFiles = errorStore.findStoredFiles();
        assertEquals(0, storedFiles.size());

        errorStore.cancelQueuedFiles(ogFiles);
        storedFiles = errorStore.findStoredFiles();
        assertEquals(1, storedFiles.size());
    }

}
