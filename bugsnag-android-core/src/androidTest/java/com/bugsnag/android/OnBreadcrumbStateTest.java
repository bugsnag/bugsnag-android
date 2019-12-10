package com.bugsnag.android;

import static com.bugsnag.android.BugsnagTestUtils.generateClient;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import androidx.annotation.NonNull;
import androidx.test.filters.SmallTest;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.HashMap;

@SmallTest
public class OnBreadcrumbStateTest {

    private Client client;

    /**
     * Configures a client which does not automatically record breadcrumbs
     *
     */
    @Before
    public void setUp() {
        Configuration configuration = new Configuration("api-key");
        configuration.setEnabledBreadcrumbTypes(Collections.<BreadcrumbType>emptySet());
        client = generateClient();
        assertEquals(1, client.breadcrumbState.getStore().size());
    }

    @After
    public void tearDown() {
        client.close();
    }

    @Test
    public void noCallback() {
        client.leaveBreadcrumb("Hello");
        assertEquals(2, client.breadcrumbState.getStore().size());
    }

    @Test
    public void falseCallback() {
        client.addOnBreadcrumb(new OnBreadcrumb() {
            @Override
            public boolean run(@NonNull Breadcrumb breadcrumb) {
                return false;
            }
        });
        client.leaveBreadcrumb("Hello");
        assertEquals(1, client.breadcrumbState.getStore().size());
    }

    @Test
    public void trueCallback() {
        client.addOnBreadcrumb(new OnBreadcrumb() {
            @Override
            public boolean run(@NonNull Breadcrumb breadcrumb) {
                return true;
            }
        });
        client.leaveBreadcrumb("Hello");
        assertEquals(2, client.breadcrumbState.getStore().size());
    }

    @Test
    public void multipleCallbacks() {
        client.addOnBreadcrumb(new OnBreadcrumb() {
            @Override
            public boolean run(@NonNull Breadcrumb breadcrumb) {
                return true;
            }
        });
        client.addOnBreadcrumb(new OnBreadcrumb() {
            @Override
            public boolean run(@NonNull Breadcrumb breadcrumb) {
                return false;
            }
        });
        client.leaveBreadcrumb("Hello");
        assertEquals(1, client.breadcrumbState.getStore().size());
    }

    @Test
    public void ensureBothCalled() {
        final int[] count = {1};
        client.addOnBreadcrumb(new OnBreadcrumb() {
            @Override
            public boolean run(@NonNull Breadcrumb breadcrumb) {
                count[0] += 1;
                return true;
            }
        });
        client.addOnBreadcrumb(new OnBreadcrumb() {
            @Override
            public boolean run(@NonNull Breadcrumb breadcrumb) {
                count[0] += 1;
                return true;
            }
        });

        client.leaveBreadcrumb("Foo");
        client.leaveBreadcrumb("Hello", BreadcrumbType.USER, new HashMap<String, Object>());
        assertEquals(5, count[0]);
    }

    @Test
    public void ensureOnlyCalledOnce() {
        final int[] count = {1};

        OnBreadcrumb onBreadcrumb = new OnBreadcrumb() {
            @Override
            public boolean run(@NonNull Breadcrumb breadcrumb) {
                count[0] += 1;
                return true;
            }
        };
        client.addOnBreadcrumb(onBreadcrumb);
        client.addOnBreadcrumb(onBreadcrumb);
        client.leaveBreadcrumb("Foo");
        assertEquals(2, count[0]);
    }

    @Test
    public void checkBreadcrumbFields() {
        final int[] count = {1};

        OnBreadcrumb onBreadcrumb = new OnBreadcrumb() {
            @Override
            public boolean run(@NonNull Breadcrumb breadcrumb) {
                count[0] += 1;
                assertEquals("Hello", breadcrumb.getMessage());
                assertEquals(BreadcrumbType.MANUAL, breadcrumb.getType());
                assertFalse(breadcrumb.getMetadata().isEmpty());
                return true;
            }
        };
        client.addOnBreadcrumb(onBreadcrumb);
        client.leaveBreadcrumb("Hello");
        assertEquals(2, count[0]);
    }

    @Test
    public void removedCallback() {
        OnBreadcrumb cb = new OnBreadcrumb() {
            @Override
            public boolean run(@NonNull Breadcrumb breadcrumb) {
                return false;
            }
        };
        client.addOnBreadcrumb(cb);
        client.leaveBreadcrumb("Hello");
        client.removeOnBreadcrumb(cb);
        client.leaveBreadcrumb("Hello");
        assertEquals(2, client.breadcrumbState.getStore().size());
    }

}
