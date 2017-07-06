package com.bugsnag.android;


import java.io.File;

public class ClientRejectedExecutionTest extends BugsnagTestCase {

    private static final int MAX_ALLOWED_TASKS = 128;
    private static final int TASK_COUNT = MAX_ALLOWED_TASKS * 2;

    private Client client;
    private File errorStorageDir;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        client = new Client(getContext(), "api-key");
        ErrorStore errorStore = client.errorStore;
        errorStorageDir = new File(errorStore.path);
        FileUtils.clearFilesInDir(errorStorageDir);
    }

    @Override
    public void tearDown() throws Exception {
        super.tearDown();
        FileUtils.clearFilesInDir(errorStorageDir);
    }

    /**
     * Checks that an exception is not thrown when the max task queue is reached, and that the
     * error is written to disk
     */
    public void testRejectedExecution() throws Exception {
        assertEquals(0, errorStorageDir.listFiles().length);

        for (int k = 0; k < TASK_COUNT; k++) {
            client.notify(new Throwable("Rejected Execution"));
        }

        // the exact number of files is indeterminate (dependent on the ThreadPoolExecutor)
        File[] files = errorStorageDir.listFiles();
        assertTrue(files.length > 0);
        File errorFile = files[0];
        ErrorStoreTest.checkFirstErrorReportFile(errorFile);
    }
}
