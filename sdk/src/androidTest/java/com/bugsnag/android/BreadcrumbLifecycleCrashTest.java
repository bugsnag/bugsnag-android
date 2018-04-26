package com.bugsnag.android;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class BreadcrumbLifecycleCrashTest {

    private SessionTracker sessionTracker;

    @Before
    public void setUp() throws Exception {
        Configuration configuration = new Configuration("api-key");
        Context context = InstrumentationRegistry.getContext();
        SessionStore sessionStore = new SessionStore(configuration, context);
        SessionTrackingApiClient apiClient = BugsnagTestUtils.generateSessionTrackingApiClient();
        sessionTracker = new SessionTracker(configuration, null, sessionStore, apiClient);
    }

    @Test
    public void testLifecycleBreadcrumbCrash() {
        // should not crash with a null client
        sessionTracker.leaveLifecycleBreadcrumb("FooActivity", "onCreate");
    }

}
