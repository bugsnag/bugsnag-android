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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

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
