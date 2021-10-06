package com.bugsnag.android;

import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bugsnag.android.internal.StateObserver;

import androidx.annotation.NonNull;

import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("ConstantConditions")
@RunWith(MockitoJUnitRunner.class)
public class ClientFacadeTest {

    @Mock
    ClientInternal clientInternal;

    private Client client;
    private InterceptingLogger logger;

    /**
     * Constructs a client with mock dependencies
     */
    @Before
    public void setUp() {
        logger = new InterceptingLogger();
        when(clientInternal.getLogger()).thenReturn(logger);
        client = new Client(clientInternal);
    }

    @Test
    public void startSessionValid() {
        client.startSession();
        verify(clientInternal, times(1)).startSession();
    }

    @Test
    public void pauseSessionValid() {
        client.pauseSession();
        verify(clientInternal, times(1)).pauseSession();
    }

    @Test
    public void resumeSessionValid() {
        client.resumeSession();
        verify(clientInternal, times(1)).resumeSession();
    }

    @Test
    public void setContextValid() {
        client.setContext("foo");
        verify(clientInternal, times(1)).setContext("foo");
        client.setContext(null);
        verify(clientInternal, times(1)).setContext(null);
    }

    @Test
    public void setUserValid() {
        client.setUser("123", "joe@example.com", "Joe");
        verify(clientInternal, times(1)).setUser("123", "joe@example.com", "Joe");

        client.setUser(null, null, null);
        verify(clientInternal, times(1)).setUser(null, null, null);
    }

    @Test
    public void addOnErrorValid() {
        OnErrorCallback cb = new OnErrorCallback() {
            @Override
            public boolean onError(@NonNull Event event) {
                return false;
            }
        };
        client.addOnError(cb);
        verify(clientInternal, times(1)).addOnError(cb);
    }

    @Test
    public void addOnErrorInvalid() {
        client.addOnError(null);
        verify(clientInternal, times(0)).addOnError(null);
        assertNotNull(logger.getMsg());
    }

    @Test
    public void removeOnErrorValid() {
        OnErrorCallback cb = new OnErrorCallback() {
            @Override
            public boolean onError(@NonNull Event event) {
                return false;
            }
        };
        client.removeOnError(cb);
        verify(clientInternal, times(1)).removeOnError(cb);
    }

    @Test
    public void removeOnErrorInvalid() {
        client.removeOnError(null);
        verify(clientInternal, times(0)).removeOnError(null);
        assertNotNull(logger.getMsg());
    }

    @Test
    public void addOnBreadcrumbValid() {
        OnBreadcrumbCallback cb = new OnBreadcrumbCallback() {
            @Override
            public boolean onBreadcrumb(@NonNull Breadcrumb breadcrumb) {
                return false;
            }
        };
        client.addOnBreadcrumb(cb);
        verify(clientInternal, times(1)).addOnBreadcrumb(cb);
    }

    @Test
    public void addOnBreadcrumbInvalid() {
        client.addOnBreadcrumb(null);
        verify(clientInternal, times(0)).addOnBreadcrumb(null);
        assertNotNull(logger.getMsg());
    }

    @Test
    public void removeOnBreadcrumbValid() {
        OnBreadcrumbCallback cb = new OnBreadcrumbCallback() {
            @Override
            public boolean onBreadcrumb(@NonNull Breadcrumb breadcrumb) {
                return false;
            }
        };
        client.removeOnBreadcrumb(cb);
        verify(clientInternal, times(1)).removeOnBreadcrumb(cb);
    }

    @Test
    public void removeOnBreadcrumbInvalid() {
        client.removeOnBreadcrumb(null);
        verify(clientInternal, times(0)).removeOnBreadcrumb(null);
        assertNotNull(logger.getMsg());
    }

    @Test
    public void addOnSessionValid() {
        OnSessionCallback cb = new OnSessionCallback() {
            @Override
            public boolean onSession(@NonNull Session session) {
                return false;
            }
        };
        client.addOnSession(cb);
        verify(clientInternal, times(1)).addOnSession(cb);
    }

    @Test
    public void addOnSessionInvalid() {
        client.addOnSession(null);
        verify(clientInternal, times(0)).addOnSession(null);
        assertNotNull(logger.getMsg());
    }

    @Test
    public void removeOnSessionValid() {
        OnSessionCallback cb = new OnSessionCallback() {
            @Override
            public boolean onSession(@NonNull Session session) {
                return false;
            }
        };
        client.removeOnSession(cb);
        verify(clientInternal, times(1)).removeOnSession(cb);
    }

    @Test
    public void removeOnSessionInvalid() {
        client.removeOnSession(null);
        verify(clientInternal, times(0)).removeOnSession(null);
        assertNotNull(logger.getMsg());
    }

    @Test
    public void notifyValid() {
        RuntimeException exc = new RuntimeException();
        client.notify(exc);
        verify(clientInternal, times(1)).notify(exc, null);
    }

    @Test
    public void notifyInvalid() {
        client.notify(null);
        verify(clientInternal, times(0)).notify(any(Throwable.class), any(OnErrorCallback.class));
        assertNotNull(logger.getMsg());
    }

    @Test
    public void notifyCallbackValid() {
        RuntimeException exc = new RuntimeException();
        client.notify(exc, null);
        verify(clientInternal, times(1)).notify(exc, null);
    }

    @Test
    public void notifyCallbackInvalid() {
        client.notify(null, null);
        verify(clientInternal, times(0)).notify(any(Throwable.class), any(OnErrorCallback.class));
        assertNotNull(logger.getMsg());
    }

    @Test
    public void addMetadataValid() {
        Map<String, Boolean> map = Collections.singletonMap("test", true);
        client.addMetadata("foo", map);
        verify(clientInternal, times(1)).addMetadata("foo", map);
    }

    @Test
    public void addMetadataInvalid1() {
        client.addMetadata("foo", null);
        verify(clientInternal, times(0)).addMetadata("foo", null);
        assertNotNull(logger.getMsg());
    }

    @Test
    public void addMetadataValueValid() {
        client.addMetadata("foo", "test", true);
        verify(clientInternal, times(1)).addMetadata("foo", "test", true);
    }

    @Test
    public void addMetadataValueInvalid1() {
        client.addMetadata(null, "test", true);
        verify(clientInternal, times(0)).addMetadata(null, "test", true);
        assertNotNull(logger.getMsg());
    }

    @Test
    public void addMetadataValueInvalid2() {
        client.addMetadata("foo", null, true);
        verify(clientInternal, times(0)).addMetadata("foo", null, true);
        assertNotNull(logger.getMsg());
    }

    @Test
    public void clearMetadataValid() {
        client.addMetadata("foo", "test", true);
        client.clearMetadata("foo");
        verify(clientInternal, times(1)).clearMetadata("foo");
    }

    @Test
    public void clearMetadataInvalid() {
        client.clearMetadata(null);
        verify(clientInternal, times(0)).clearMetadata(null);
        assertNotNull(logger.getMsg());
    }

    @Test
    public void clearMetadataValueValid() {
        client.addMetadata("foo", "test", true);
        client.clearMetadata("foo", "test");
        verify(clientInternal, times(1)).clearMetadata("foo", "test");
    }

    @Test
    public void clearMetadataValueInvalid1() {
        client.addMetadata("foo", "test", true);
        client.clearMetadata(null, "test");
        verify(clientInternal, times(0)).clearMetadata(null, "test");
        assertNotNull(logger.getMsg());
    }

    @Test
    public void clearMetadataValueInvalid2() {
        client.addMetadata("foo", "test", true);
        client.clearMetadata("foo", null);
        verify(clientInternal, times(0)).clearMetadata("foo", null);
        assertNotNull(logger.getMsg());
    }

    @Test
    public void getMetadataValid() {
        client.getMetadata("foo", "test");
        verify(clientInternal, times(1)).getMetadata("foo", "test");
    }

    @Test
    public void getMetadataInvalid() {
        client.getMetadata(null);
        assertNotNull(logger.getMsg());
    }

    @Test
    public void getMetadataValueValid() {
        client.addMetadata("foo", "test", true);
        client.getMetadata("foo", "test");
        verify(clientInternal, times(1)).getMetadata("foo", "test");
    }

    @Test
    public void getMetadataValueInvalid1() {
        client.addMetadata("foo", "test", true);
        client.getMetadata(null, "test");
        verify(clientInternal, times(0)).getMetadata(null, "test");
        assertNotNull(logger.getMsg());
    }

    @Test
    public void getMetadataValueInvalid2() {
        client.addMetadata("foo", "test", true);
        client.getMetadata("foo", null);
        verify(clientInternal, times(0)).getMetadata("foo", null);
        assertNotNull(logger.getMsg());
    }

    @Test
    public void leaveBreadcrumbValid() {
        client.leaveBreadcrumb("foo");
        verify(clientInternal, times(1)).leaveBreadcrumb("foo");
    }

    @Test
    public void leaveBreadcrumbInvalid() {
        client.leaveBreadcrumb(null);
        verify(clientInternal, times(0)).leaveBreadcrumb(null);
        assertNotNull(logger.getMsg());
    }

    @Test
    public void leaveComplexBreadcrumbValid() {
        HashMap<String, Object> metadata = new HashMap<>();
        client.leaveBreadcrumb("foo", metadata, BreadcrumbType.NAVIGATION);
        verify(clientInternal, times(1))
                .leaveBreadcrumb("foo", metadata, BreadcrumbType.NAVIGATION);
    }

    @Test
    public void leaveComplexBreadcrumbInvalid1() {
        HashMap<String, Object> metadata = new HashMap<>();
        client.leaveBreadcrumb(null, metadata, BreadcrumbType.NAVIGATION);
        verify(clientInternal, times(0)).leaveBreadcrumb(null, metadata, BreadcrumbType.NAVIGATION);
    }

    @Test
    public void leaveComplexBreadcrumbInvalid2() {
        HashMap<String, Object> metadata = new HashMap<>();
        client.leaveBreadcrumb("foo", metadata, null);
        verify(clientInternal, times(0)).leaveBreadcrumb("foo", metadata, null);
    }

    @Test
    public void leaveComplexBreadcrumbInvalid3() {
        client.leaveBreadcrumb("foo", null, BreadcrumbType.NAVIGATION);
        verify(clientInternal, times(0)).leaveBreadcrumb("foo", null, BreadcrumbType.NAVIGATION);
    }

    @Test
    public void addRuntimeVersionInfo() {
        client.addRuntimeVersionInfo("foo", "bar");
        verify(clientInternal, times(1)).addRuntimeVersionInfo("foo", "bar");
    }

    @Test
    public void markLaunchCompleted() {
        client.markLaunchCompleted();
        verify(clientInternal, times(1)).markLaunchCompleted();
    }

    @Test
    public void registerObserver() {
        StateObserver observer = new StateObserver() {
            @Override
            public void onStateChange(@NotNull StateEvent event) {
            }
        };
        client.addObserver(observer);
        verify(clientInternal, times(1)).addObserver(observer);
    }

    @Test
    public void unregisterObserver() {
        StateObserver observer = new StateObserver() {
            @Override
            public void onStateChange(@NotNull StateEvent event) {
            }
        };
        client.removeObserver(observer);
        verify(clientInternal, times(1)).removeObserver(observer);
    }

}
