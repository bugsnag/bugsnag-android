package com.bugsnag.android;

import android.support.annotation.NonNull;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.HashMap;
import java.util.Map;

import static junit.framework.Assert.assertEquals;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class BeforeBreadcrumbsTest {

    private Client client;

    @Before
    public void setUp() throws Exception {
        Configuration configuration = new Configuration("api-key");
        configuration.setAutomaticallyCollectBreadcrumbs(false);
        client = new Client(InstrumentationRegistry.getContext(), configuration);
        assertEquals(0, client.breadcrumbs.store.size());
    }

    @Test
    public void noCallback() throws Exception {
        client.leaveBreadcrumb("Hello");
        assertEquals(1, client.breadcrumbs.store.size());
    }

    @Test
    public void falseCallback() throws Exception {
        client.beforeBreadcrumb(new BeforeBreadcrumb() {
            @Override
            public boolean send(@NonNull String name, @NonNull BreadcrumbType breadcrumbType, @NonNull Map<String, String> metadata) {
                return false;
            }
        });
        client.leaveBreadcrumb("Hello");
        assertEquals(0, client.breadcrumbs.store.size());
    }

    @Test
    public void trueCallback() throws Exception {
        client.beforeBreadcrumb(new BeforeBreadcrumb() {
            @Override
            public boolean send(@NonNull String name, @NonNull BreadcrumbType breadcrumbType, @NonNull Map<String, String> metadata) {
                return true;
            }
        });
        client.leaveBreadcrumb("Hello");
        assertEquals(1, client.breadcrumbs.store.size());
    }

    @Test
    public void multipleCallbacks() throws Exception {
        client.beforeBreadcrumb(new BeforeBreadcrumb() {
            @Override
            public boolean send(@NonNull String name, @NonNull BreadcrumbType breadcrumbType, @NonNull Map<String, String> metadata) {
                return true;
            }
        });
        client.beforeBreadcrumb(new BeforeBreadcrumb() {
            @Override
            public boolean send(@NonNull String name, @NonNull BreadcrumbType breadcrumbType, @NonNull Map<String, String> metadata) {
                return false;
            }
        });
        client.leaveBreadcrumb("Hello");
        assertEquals(0, client.breadcrumbs.store.size());
    }

    @Test
    public void ensureBothCalled() throws Exception {
        final int[] count = {0};
        client.beforeBreadcrumb(new BeforeBreadcrumb() {
            @Override
            public boolean send(@NonNull String name, @NonNull BreadcrumbType breadcrumbType, @NonNull Map<String, String> metadata) {
                count[0] += 1;
                return true;
            }
        });
        client.beforeBreadcrumb(new BeforeBreadcrumb() {
            @Override
            public boolean send(@NonNull String name, @NonNull BreadcrumbType breadcrumbType, @NonNull Map<String, String> metadata) {
                count[0] += 1;
                return true;
            }
        });

        client.leaveBreadcrumb("Foo");
        client.leaveBreadcrumb("Hello", BreadcrumbType.USER, new HashMap<String, String>());
        client.leaveBreadcrumb("Hello", BreadcrumbType.USER, new HashMap<String, String>(), false);
        assertEquals(6, count[0]);
    }

    @Test
    public void ensureOnlyCalledOnce() throws Exception {
        final int[] count = {0};

        BeforeBreadcrumb beforeBreadcrumb = new BeforeBreadcrumb() {
            @Override
            public boolean send(@NonNull String name, @NonNull BreadcrumbType breadcrumbType, @NonNull Map<String, String> metadata) {
                count[0] += 1;
                return true;
            }
        };
        client.beforeBreadcrumb(beforeBreadcrumb);
        client.beforeBreadcrumb(beforeBreadcrumb);
        client.leaveBreadcrumb("Foo");
        assertEquals(1, count[0]);
    }

}
