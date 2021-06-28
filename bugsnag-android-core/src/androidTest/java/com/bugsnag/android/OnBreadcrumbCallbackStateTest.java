package com.bugsnag.android;

import static com.bugsnag.android.BugsnagTestUtils.generateClient;
import static com.bugsnag.android.BugsnagTestUtils.generateConfiguration;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import androidx.annotation.NonNull;
import androidx.test.filters.SmallTest;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

@SmallTest
public class OnBreadcrumbCallbackStateTest {

    private Client client;

    /**
     * Configures a client which does not automatically record breadcrumbs
     *
     */
    @Before
    public void setUp() {
        Configuration configuration = generateConfiguration();

        Set<BreadcrumbType> breadcrumbTypes = new HashSet<>();
        breadcrumbTypes.add(BreadcrumbType.MANUAL);
        breadcrumbTypes.add(BreadcrumbType.USER);
        configuration.setEnabledBreadcrumbTypes(breadcrumbTypes);
        client = generateClient(configuration);
        assertEquals(0, client.breadcrumbState.copy().size());
    }

    @After
    public void tearDown() {
        client.close();
    }

    @Test
    public void noCallback() {
        client.leaveBreadcrumb("Hello");
        assertEquals(1, client.breadcrumbState.copy().size());
    }

    @Test
    public void falseCallback() {
        client.addOnBreadcrumb(new OnBreadcrumbCallback() {
            @Override
            public boolean onBreadcrumb(@NonNull Breadcrumb breadcrumb) {
                return false;
            }
        });
        client.leaveBreadcrumb("Hello");
        assertEquals(0, client.breadcrumbState.copy().size());
    }

    @Test
    public void trueCallback() {
        client.addOnBreadcrumb(new OnBreadcrumbCallback() {
            @Override
            public boolean onBreadcrumb(@NonNull Breadcrumb breadcrumb) {
                return true;
            }
        });
        client.leaveBreadcrumb("Hello");
        assertEquals(1, client.breadcrumbState.copy().size());
    }

    @Test
    public void multipleCallbacks() {
        client.addOnBreadcrumb(new OnBreadcrumbCallback() {
            @Override
            public boolean onBreadcrumb(@NonNull Breadcrumb breadcrumb) {
                return true;
            }
        });
        client.addOnBreadcrumb(new OnBreadcrumbCallback() {
            @Override
            public boolean onBreadcrumb(@NonNull Breadcrumb breadcrumb) {
                return false;
            }
        });
        client.leaveBreadcrumb("Hello");
        assertEquals(0, client.breadcrumbState.copy().size());
    }

    @Test
    public void ensureBothCalled() {
        final int[] count = {1};
        client.addOnBreadcrumb(new OnBreadcrumbCallback() {
            @Override
            public boolean onBreadcrumb(@NonNull Breadcrumb breadcrumb) {
                count[0] += 1;
                return true;
            }
        });
        client.addOnBreadcrumb(new OnBreadcrumbCallback() {
            @Override
            public boolean onBreadcrumb(@NonNull Breadcrumb breadcrumb) {
                count[0] += 1;
                return true;
            }
        });

        client.leaveBreadcrumb("Foo");
        client.leaveBreadcrumb("Hello", new HashMap<String, Object>(), BreadcrumbType.USER);
        assertEquals(5, count[0]);
    }

    @Test
    public void ensureCalledTwice() {
        final int[] count = {1};

        OnBreadcrumbCallback onBreadcrumb = new OnBreadcrumbCallback() {
            @Override
            public boolean onBreadcrumb(@NonNull Breadcrumb breadcrumb) {
                count[0] += 1;
                return true;
            }
        };
        client.addOnBreadcrumb(onBreadcrumb);
        client.addOnBreadcrumb(onBreadcrumb);
        client.leaveBreadcrumb("Foo");
        assertEquals(3, count[0]);
    }

    @Test
    public void checkBreadcrumbFields() {
        final int[] count = {1};

        OnBreadcrumbCallback onBreadcrumb = new OnBreadcrumbCallback() {
            @Override
            public boolean onBreadcrumb(@NonNull Breadcrumb breadcrumb) {
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
        OnBreadcrumbCallback cb = new OnBreadcrumbCallback() {
            @Override
            public boolean onBreadcrumb(@NonNull Breadcrumb breadcrumb) {
                return false;
            }
        };
        client.addOnBreadcrumb(cb);
        client.leaveBreadcrumb("Hello");
        client.removeOnBreadcrumb(cb);
        client.leaveBreadcrumb("Hello");
        assertEquals(1, client.breadcrumbState.copy().size());
    }

}
