package com.bugsnag.android;

import android.support.annotation.NonNull;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Collection;

/**
 * Ensures that if a callback is added or removed during iteration, a
 * {@link java.util.ConcurrentModificationException} is not thrown
 */
@RunWith(AndroidJUnit4.class)
@SmallTest
public class ConcurrentCallbackTest {

    private Client client;

    @Before
    public void setUp() throws Exception {
        client = BugsnagTestUtils.generateClient();
    }

    @After
    public void tearDown() throws Exception {
        client.close();
    }

    @Test
    public void testClientNotifyModification() throws Exception {
        Configuration config = (Configuration) client.getConfiguration();
        final Collection<BeforeNotify> beforeNotifyTasks = config.getBeforeNotifyTasks();
        client.addBeforeNotify(new BeforeNotify() {
            @Override
            public boolean run(@NonNull Error error) {
                beforeNotifyTasks.add(new BeforeNotifySkeleton());
                // modify the Set, when iterating to the next callback this should not crash
                return true;
            }
        });
        client.addBeforeNotify(new BeforeNotifySkeleton());
        client.notify(new RuntimeException());
    }

    @Test
    public void testClientBreadcrumbModification() throws Exception {
        Configuration config = (Configuration) client.getConfiguration();
        final Collection<BeforeRecordBreadcrumb> breadcrumbTasks =
                config.getBeforeRecordBreadcrumbTasks();

        client.beforeRecordBreadcrumb(new BeforeRecordBreadcrumb() {
            @Override
            public boolean shouldRecord(@NonNull Breadcrumb breadcrumb) {
                breadcrumbTasks.add(new BeforeRecordBreadcrumbSkeleton());
                // modify the Set, when iterating to the next callback this should not crash
                return true;
            }
        });
        client.beforeRecordBreadcrumb(new BeforeRecordBreadcrumbSkeleton());
        client.leaveBreadcrumb("Whoops");
        client.notify(new RuntimeException());
    }

    static class BeforeNotifySkeleton implements BeforeNotify {
        @Override
        public boolean run(@NonNull Error error) {
            return true;
        }
    }

    static class BeforeRecordBreadcrumbSkeleton implements BeforeRecordBreadcrumb {
        @Override
        public boolean shouldRecord(@NonNull Breadcrumb breadcrumb) {
            return true;
        }
    }

}
