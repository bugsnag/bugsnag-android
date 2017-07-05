package com.bugsnag.android;

import org.json.JSONObject;

import java.io.File;
import java.io.StringWriter;

public class ErrorStoreTest extends BugsnagTestCase {

    private ErrorStore errorStore;
    private Configuration config;
    private File errorStorageDir;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        Client client = new Client(getContext(), "api-key");
        config = client.config;
        errorStore = client.errorStore;
        errorStorageDir = new File(errorStore.path);
        FileUtils.clearFilesInDir(errorStorageDir);
    }

    @Override
    public void tearDown() throws Exception {
        super.tearDown();
        FileUtils.clearFilesInDir(errorStorageDir);
    }

    public void testWrite() throws Exception {
        Error error = new Error(config, new RuntimeException());
        errorStore.write(error);

        File[] files = errorStorageDir.listFiles();
        assertEquals(1, files.length);
        File file = files[0];
        checkFirstErrorReportFile(file);
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
