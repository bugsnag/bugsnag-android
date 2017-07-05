package com.bugsnag.android;

import org.json.JSONObject;

import java.io.File;
import java.io.StringWriter;

public class ClientRejectedExecutionTest extends BugsnagTestCase {

    private static final int MAX_ALLOWED_TASKS = 128;
    private static final int TASK_COUNT = MAX_ALLOWED_TASKS * 2;
    public static final String ERROR_MESSAGE = "Rejected Execution";

    private Client client;
    private File errorStorageDir;

    @Override
    public void setUp() throws Exception {
        super.setUp();

        client = new Client(getContext(), "api-key");
        ErrorStore errorStore = client.errorStore;
        errorStorageDir = new File(errorStore.path);
        clearFilesInDir(errorStorageDir);
    }

    private void clearFilesInDir(File storageDir) {
        if (!storageDir.isDirectory()) {
            throw new IllegalArgumentException();
        }
        for (File file : storageDir.listFiles()) {
            if (!file.isDirectory()) {
                file.delete();
            }
        }
    }

    /**
     * Checks that an exception is not thrown when the max task queue is reached, and that the
     * error is written to disk
     */
    public void testRejectedExecution() throws Exception {
        assertEquals(0, errorStorageDir.listFiles().length);

        for (int k = 0; k < TASK_COUNT; k++) {
            client.notify(new Throwable(ERROR_MESSAGE));
        }
        checkFirstErrorReportFile();
    }

    private void checkFirstErrorReportFile() throws Exception {
        // the exact number of files is indeterminate (dependent on the ThreadPoolExecutor)
        File[] files = errorStorageDir.listFiles();
        assertTrue(files.length > 0);

        // ensure that the file isn't empty
        File errorFile = files[0];
        assertFalse(errorFile.length() <= 0);

        // ensure the file can be serialised into JSON report
        Report report = new Report("abc", errorFile);
        StringWriter stringWriter = new StringWriter();
        report.toStream(new JsonStream(stringWriter));
        JSONObject jsonObject = new JSONObject(stringWriter.toString());
        assertNotNull(jsonObject);
    }

}
