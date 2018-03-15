package com.bugsnag.android;

import android.support.test.InstrumentationRegistry;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;


/**
 * Tests that only 1 request is sent in the case where stored reports are concurrently flushed.
 * This can occur in the following scenarios:
 * <p>
 * - Client initialisation
 * - Network connectivity changes
 * - When catching an unhandled exception
 */
@RunWith(AndroidJUnit4.class)
@SmallTest
public class ErrorStoreAsyncTest {

    private ErrorStore errorStore;
    private Configuration config;
    private File errorStorageDir;
    private ErrorReportApiClient apiClient;
    private AtomicInteger requestCount;

    @Before
    public void setUp() throws Exception {
        requestCount = new AtomicInteger();

        apiClient = new ErrorReportApiClient() {
            @Override
            public void postReport(String urlString,
                                   Report report,
                                   Map<String, String> headers)
                throws NetworkException, BadResponseException {
                requestCount.incrementAndGet();
                try {
                    Thread.sleep(100); // simulate long network request
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        };

        Client client = new Client(InstrumentationRegistry.getContext(), "api-key");
        client.setErrorReportApiClient(apiClient);

        config = client.config;
        errorStore = client.errorStore;
        assertNotNull(errorStore.storeDirectory);
        errorStorageDir = new File(errorStore.storeDirectory);
        FileUtils.clearFilesInDir(errorStorageDir);
    }

    private void writeFakeError() {
        errorStore.write(new Error.Builder(config, new RuntimeException(), null).build());
    }

    @After
    public void tearDown() throws Exception {
        FileUtils.clearFilesInDir(errorStorageDir);
    }

    @Test
    public void testConnectivityThenLaunch() throws Exception {
        writeFakeError();
        errorStore.flushAsync(apiClient);
        errorStore.flushOnLaunch(apiClient);
        assertEquals(1, requestCount.get());
    }

    @Test
    public void testLaunchThenConnectivity() throws Exception {
        writeFakeError();
        errorStore.flushOnLaunch(apiClient);
        errorStore.flushAsync(apiClient);
        assertEquals(1, requestCount.get());
    }

    @Test
    public void testMultiFlushAsync() throws Exception {
        writeFakeError();
        errorStore.flushAsync(apiClient);
        errorStore.flushAsync(apiClient);
        Thread.sleep(10);
        assertEquals(1, requestCount.get());
    }
}
