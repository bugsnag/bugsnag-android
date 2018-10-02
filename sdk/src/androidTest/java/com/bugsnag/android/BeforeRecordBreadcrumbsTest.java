package com.bugsnag.android;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;

import android.support.annotation.NonNull;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.HashMap;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class BeforeRecordBreadcrumbsTest {

    private Client client;

    /**
     * Configures a client which does not automatically record breadcrumbs
     *
     * @throws Exception if initialisation failed
     */
    @Before
    public void setUp() throws Exception {
        Configuration configuration = new Configuration("api-key");
        configuration.setAutomaticallyCollectBreadcrumbs(false);
        client = new Client(InstrumentationRegistry.getContext(), configuration);
        assertEquals(0, client.breadcrumbs.store.size());
    }

    @After
    public void tearDown() throws Exception {
        Async.cancelTasks();
        client.getOrientationListener().disable();
    }

    @Test
    public void noCallback() throws Exception {
        client.leaveBreadcrumb("Hello");
        assertEquals(1, client.breadcrumbs.store.size());
    }

    @Test
    public void falseCallback() throws Exception {
        client.beforeRecordBreadcrumb(new BeforeRecordBreadcrumb() {
            @Override
            public boolean shouldRecord(@NonNull Breadcrumb breadcrumb) {
                return false;
            }
        });
        client.leaveBreadcrumb("Hello");
        assertEquals(0, client.breadcrumbs.store.size());
    }

    @Test
    public void trueCallback() throws Exception {
        client.beforeRecordBreadcrumb(new BeforeRecordBreadcrumb() {
            @Override
            public boolean shouldRecord(@NonNull Breadcrumb breadcrumb) {
                return true;
            }
        });
        client.leaveBreadcrumb("Hello");
        assertEquals(1, client.breadcrumbs.store.size());
    }

    @Test
    public void multipleCallbacks() throws Exception {
        client.beforeRecordBreadcrumb(new BeforeRecordBreadcrumb() {
            @Override
            public boolean shouldRecord(@NonNull Breadcrumb breadcrumb) {
                return true;
            }
        });
        client.beforeRecordBreadcrumb(new BeforeRecordBreadcrumb() {
            @Override
            public boolean shouldRecord(@NonNull Breadcrumb breadcrumb) {
                return false;
            }
        });
        client.leaveBreadcrumb("Hello");
        assertEquals(0, client.breadcrumbs.store.size());
    }

    @Test
    public void ensureBothCalled() throws Exception {
        final int[] count = {0};
        client.beforeRecordBreadcrumb(new BeforeRecordBreadcrumb() {
            @Override
            public boolean shouldRecord(@NonNull Breadcrumb breadcrumb) {
                count[0] += 1;
                return true;
            }
        });
        client.beforeRecordBreadcrumb(new BeforeRecordBreadcrumb() {
            @Override
            public boolean shouldRecord(@NonNull Breadcrumb breadcrumb) {
                count[0] += 1;
                return true;
            }
        });

        client.leaveBreadcrumb("Foo");
        client.leaveBreadcrumb("Hello", BreadcrumbType.USER, new HashMap<String, String>());
        assertEquals(4, count[0]);
    }

    @Test
    public void ensureOnlyCalledOnce() throws Exception {
        final int[] count = {0};

        BeforeRecordBreadcrumb beforeRecordBreadcrumb = new BeforeRecordBreadcrumb() {
            @Override
            public boolean shouldRecord(@NonNull Breadcrumb breadcrumb) {
                count[0] += 1;
                return true;
            }
        };
        client.beforeRecordBreadcrumb(beforeRecordBreadcrumb);
        client.beforeRecordBreadcrumb(beforeRecordBreadcrumb);
        client.leaveBreadcrumb("Foo");
        assertEquals(1, count[0]);
    }

    @Test
    public void checkBreadrumbFields() throws Exception {
        final int[] count = {0};

        BeforeRecordBreadcrumb beforeRecordBreadcrumb = new BeforeRecordBreadcrumb() {
            @Override
            public boolean shouldRecord(@NonNull Breadcrumb breadcrumb) {
                count[0] += 1;
                assertEquals("Hello", breadcrumb.getName());
                assertEquals(BreadcrumbType.MANUAL, breadcrumb.getType());
                assertFalse(breadcrumb.getMetadata().isEmpty());
                return true;
            }
        };
        client.beforeRecordBreadcrumb(beforeRecordBreadcrumb);
        client.leaveBreadcrumb("Hello");
        assertEquals(1, count[0]);
    }

}
