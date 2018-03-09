package com.bugsnag.android;

import android.support.annotation.NonNull;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

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

    @Test
    public void testClientNotifyModification() throws Exception {
        final Collection<BeforeNotify> beforeNotifyTasks = client.config.getBeforeNotifyTasks();
        client.beforeNotify(new BeforeNotify() {
            @Override
            public boolean run(Error error) {
                beforeNotifyTasks.clear();
                return true;
            }
        });
        client.beforeNotify(new BeforeNotify() {
            @Override
            public boolean run(Error error) {
                return true;
            }
        });
        client.notify(new RuntimeException());
    }

    @Test
    public void testClientBreadcrumbModification() throws Exception {
        final Collection<BeforeRecordBreadcrumb> breadcrumbTasks = client.config.getBeforeRecordBreadcrumbTasks();

        client.beforeRecordBreadcrumb(new BeforeRecordBreadcrumb() {
            @Override
            public boolean shouldRecord(@NonNull Breadcrumb breadcrumb) {
                breadcrumbTasks.clear();
                return true;
            }
        });
        client.beforeRecordBreadcrumb(new BeforeRecordBreadcrumb() {
            @Override
            public boolean shouldRecord(@NonNull Breadcrumb breadcrumb) {
                return true;
            }
        });
        client.leaveBreadcrumb("Whoops");
        client.notify(new RuntimeException());
    }


}
