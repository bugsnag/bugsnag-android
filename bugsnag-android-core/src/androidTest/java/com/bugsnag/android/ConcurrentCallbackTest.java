package com.bugsnag.android;

import androidx.annotation.NonNull;
import androidx.test.filters.SmallTest;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Collection;

/**
 * Ensures that if a callback is added or removed during iteration, a
 * {@link java.util.ConcurrentModificationException} is not thrown
 */
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
        final Collection<OnError> onErrorTasks = config.getOnErrorTasks();
        client.addOnError(new OnError() {
            @Override
            public boolean run(@NonNull Event event) {
                onErrorTasks.add(new OnErrorSkeleton());
                // modify the Set, when iterating to the next callback this should not crash
                return true;
            }
        });
        client.addOnError(new OnErrorSkeleton());
        client.notify(new RuntimeException());
    }

    @Test
    public void testClientBreadcrumbModification() throws Exception {
        Configuration config = (Configuration) client.getConfiguration();
        final Collection<OnBreadcrumb> breadcrumbTasks =
                config.getBreadcrumbCallbacks();

        client.addOnBreadcrumb(new OnBreadcrumb() {
            @Override
            public boolean run(@NonNull Breadcrumb breadcrumb) {
                breadcrumbTasks.add(new OnBreadcrumbSkeleton());
                // modify the Set, when iterating to the next callback this should not crash
                return true;
            }
        });
        client.addOnBreadcrumb(new OnBreadcrumbSkeleton());
        client.leaveBreadcrumb("Whoops");
        client.notify(new RuntimeException());
    }

    static class OnErrorSkeleton implements OnError {
        @Override
        public boolean run(@NonNull Event event) {
            return true;
        }
    }

    static class OnBreadcrumbSkeleton implements OnBreadcrumb {
        @Override
        public boolean run(@NonNull Breadcrumb breadcrumb) {
            return true;
        }
    }

}
