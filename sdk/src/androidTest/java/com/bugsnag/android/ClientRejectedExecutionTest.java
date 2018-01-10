package com.bugsnag.android;


import android.support.test.filters.LargeTest;
import android.support.test.runner.AndroidJUnit4;

import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.util.Map;

import static junit.framework.Assert.assertNotNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class ClientRejectedExecutionTest {

    private static final int MAX_ALLOWED_TASKS = 128;
    private static final int TASK_COUNT = MAX_ALLOWED_TASKS * 4;

    private Client client;
    private File errorStorageDir;

    @Before
    public void setUp() throws Exception {
        client = BugsnagTestUtils.generateClient();
        client.setErrorReportApiClient(new ErrorReportApiClient() {
            @Override
            public void postReport(String urlString, Report report, Map<String, String> headers) throws NetworkException, BadResponseException {
                try {
                    Thread.sleep(20); // simulate network call
                } catch (InterruptedException ignored) {
                }
            }
        });
        ErrorStore errorStore = client.errorStore;
        assertNotNull(errorStore.storeDirectory);
        errorStorageDir = new File(errorStore.storeDirectory);
        clearPendingErrors();
    }

    @After
    public void tearDown() throws Exception {
        clearPendingErrors();
    }

    private void clearPendingErrors() {
        Async.POOL_WORK_QUEUE.clear();
        FileUtils.clearFilesInDir(errorStorageDir);
    }

    /**
     * Checks that an exception is not thrown when the max task queue is reached, and that the
     * error is written to disk
     */
    @Test
    public void testRejectedExecution() throws Exception {
        assertEquals(0, errorStorageDir.listFiles().length);

        for (int k = 0; k < TASK_COUNT; k++) {
            client.notify(new Throwable("Rejected Execution"));
        }

        // the exact number of files is indeterminate (dependent on the ThreadPoolExecutor)
        File[] files = errorStorageDir.listFiles();
        assertTrue(files.length > 0);
        File errorFile = files[0];

        JSONObject payload = ErrorStoreTest.getJsonObjectFromReport(new Report(errorFile));
        ErrorStoreTest.validateReportPayload(payload);
    }
}
