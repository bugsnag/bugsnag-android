package com.bugsnag.android;

import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.storage.StorageManager;

import androidx.annotation.NonNull;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("ConstantConditions")
@RunWith(MockitoJUnitRunner.class)
public class ClientFacadeTest {

    @Mock
    ImmutableConfig immutableConfig;

    @Mock
    MetadataState metadataState;

    @Mock
    ContextState contextState;

    @Mock
    CallbackState callbackState;

    @Mock
    UserState userState;

    @Mock
    Context appContext;

    @Mock
    DeviceDataCollector deviceDataCollector;

    @Mock
    AppDataCollector appDataCollector;

    @Mock
    BreadcrumbState breadcrumbState;

    @Mock
    EventStore eventStore;

    @Mock
    SessionStore sessionStore;

    @Mock
    SystemBroadcastReceiver systemBroadcastReceiver;

    @Mock
    SessionTracker sessionTracker;

    @Mock
    ActivityBreadcrumbCollector activityBreadcrumbCollector;

    @Mock
    SessionLifecycleCallback sessionLifecycleCallback;

    @Mock
    SharedPreferences sharedPrefs;

    @Mock
    Connectivity connectivity;

    @Mock
    StorageManager storageManager;

    @Mock
    DeliveryDelegate deliveryDelegate;

    @Mock
    AppWithState app;

    @Mock
    DeviceWithState device;

    @Mock
    LastRunInfoStore lastRunInfoStore;

    @Mock
    LaunchCrashTracker launchCrashTracker;

    private Client client;
    private InterceptingLogger logger;

    /**
     * Constructs a client with mock dependencies
     */
    @Before
    public void setUp() {
        logger = new InterceptingLogger();
        client = new Client(
                immutableConfig,
                metadataState,
                contextState,
                callbackState,
                userState,
                appContext,
                deviceDataCollector,
                appDataCollector,
                breadcrumbState,
                eventStore,
                sessionStore,
                systemBroadcastReceiver,
                sessionTracker,
                activityBreadcrumbCollector,
                sessionLifecycleCallback,
                connectivity,
                storageManager,
                logger,
                deliveryDelegate,
                lastRunInfoStore,
                launchCrashTracker
        );

        // required fields for generating an event
        when(metadataState.getMetadata()).thenReturn(new Metadata());
        when(immutableConfig.getLogger()).thenReturn(logger);
        when(immutableConfig.getSendThreads()).thenReturn(ThreadSendPolicy.ALWAYS);
        when(immutableConfig.shouldNotifyForReleaseStage()).thenReturn(true);

        when(deviceDataCollector.generateDeviceWithState(anyLong())).thenReturn(device);
        when(deviceDataCollector.getDeviceMetadata()).thenReturn(new HashMap<String, Object>());
        when(appDataCollector.generateAppWithState()).thenReturn(app);
        when(appDataCollector.getAppDataMetadata()).thenReturn(new HashMap<String, Object>());

        when(breadcrumbState.getStore()).thenReturn(new ArrayDeque<Breadcrumb>());
        when(userState.getUser()).thenReturn(new User());
        when(callbackState.runOnErrorTasks(any(Event.class), any(Logger.class))).thenReturn(true);
    }

    @Test
    public void startSessionValid() {
        client.startSession();
        verify(sessionTracker, times(1)).startSession(false);
    }

    @Test
    public void pauseSessionValid() {
        client.pauseSession();
        verify(sessionTracker, times(1)).pauseSession();
    }

    @Test
    public void resumeSessionValid() {
        client.resumeSession();
        verify(sessionTracker, times(1)).resumeSession();
    }

    @Test
    public void setContextValid() {
        client.setContext("foo");
        verify(contextState, times(1)).setContext("foo");
        client.setContext(null);
        verify(contextState, times(1)).setContext(null);
    }

    @Test
    public void setUserValid() {
        client.setUser("123", "joe@example.com", "Joe");
        User joe = new User("123", "joe@example.com", "Joe");
        verify(userState, times(1)).setUser(joe);

        client.setUser(null, null, null);
        User emptyUser = new User(null, null, null);
        verify(userState, times(1)).setUser(emptyUser);
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
        verify(callbackState, times(1)).addOnError(cb);
    }

    @Test
    public void addOnErrorInvalid() {
        client.addOnError(null);
        verify(callbackState, times(0)).addOnError(null);
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
        verify(callbackState, times(1)).removeOnError(cb);
    }

    @Test
    public void removeOnErrorInvalid() {
        client.removeOnError(null);
        verify(callbackState, times(0)).removeOnError(null);
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
        verify(callbackState, times(1)).addOnBreadcrumb(cb);
    }

    @Test
    public void addOnBreadcrumbInvalid() {
        client.addOnBreadcrumb(null);
        verify(callbackState, times(0)).addOnBreadcrumb(null);
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
        verify(callbackState, times(1)).removeOnBreadcrumb(cb);
    }

    @Test
    public void removeOnBreadcrumbInvalid() {
        client.removeOnBreadcrumb(null);
        verify(callbackState, times(0)).removeOnBreadcrumb(null);
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
        verify(callbackState, times(1)).addOnSession(cb);
    }

    @Test
    public void addOnSessionInvalid() {
        client.addOnSession(null);
        verify(callbackState, times(0)).addOnSession(null);
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
        verify(callbackState, times(1)).removeOnSession(cb);
    }

    @Test
    public void removeOnSessionInvalid() {
        client.removeOnSession(null);
        verify(callbackState, times(0)).removeOnSession(null);
        assertNotNull(logger.getMsg());
    }

    @Test
    public void notifyValid() {
        RuntimeException exc = new RuntimeException();
        client.notify(exc);
        verify(deliveryDelegate, times(1)).deliver(any(Event.class));
    }

    @Test
    public void notifyInvalid() {
        client.notify(null);
        verify(deliveryDelegate, times(0)).deliver(any(Event.class));
        assertNotNull(logger.getMsg());
    }

    @Test
    public void notifyCallbackValid() {
        RuntimeException exc = new RuntimeException();
        client.notify(exc, null);
        verify(deliveryDelegate, times(1)).deliver(any(Event.class));
    }

    @Test
    public void notifyCallbackInvalid() {
        client.notify(null, null);
        verify(deliveryDelegate, times(0)).deliver(any(Event.class));
        assertNotNull(logger.getMsg());
    }

    @Test
    public void addMetadataValid() {
        Map<String, Boolean> map = Collections.singletonMap("test", true);
        client.addMetadata("foo", map);
        verify(metadataState, times(1)).addMetadata("foo", map);
    }

    @Test
    public void addMetadataInvalid1() {
        client.addMetadata("foo", null);
        verify(metadataState, times(0)).addMetadata("foo", null);
        assertNotNull(logger.getMsg());
    }

    @Test
    public void addMetadataValueValid() {
        client.addMetadata("foo", "test", true);
        verify(metadataState, times(1)).addMetadata("foo", "test", true);
    }

    @Test
    public void addMetadataValueInvalid1() {
        client.addMetadata(null, "test", true);
        verify(metadataState, times(0)).addMetadata(null, "test", true);
        assertNotNull(logger.getMsg());
    }

    @Test
    public void addMetadataValueInvalid2() {
        client.addMetadata("foo", null, true);
        verify(metadataState, times(0)).addMetadata("foo", null, true);
        assertNotNull(logger.getMsg());
    }

    @Test
    public void clearMetadataValid() {
        client.addMetadata("foo", "test", true);
        client.clearMetadata("foo");
        verify(metadataState, times(1)).clearMetadata("foo");
    }

    @Test
    public void clearMetadataInvalid() {
        client.clearMetadata(null);
        verify(metadataState, times(0)).clearMetadata(null);
        assertNotNull(logger.getMsg());
    }

    @Test
    public void clearMetadataValueValid() {
        client.addMetadata("foo", "test", true);
        client.clearMetadata("foo", "test");
        verify(metadataState, times(1)).clearMetadata("foo", "test");
    }

    @Test
    public void clearMetadataValueInvalid1() {
        client.addMetadata("foo", "test", true);
        client.clearMetadata(null, "test");
        verify(metadataState, times(0)).clearMetadata(null, "test");
        assertNotNull(logger.getMsg());
    }

    @Test
    public void clearMetadataValueInvalid2() {
        client.addMetadata("foo", "test", true);
        client.clearMetadata("foo", null);
        verify(metadataState, times(0)).clearMetadata("foo", null);
        assertNotNull(logger.getMsg());
    }

    @Test
    public void getMetadataValid() {
        client.getMetadata("foo", "test");
        verify(metadataState, times(1)).getMetadata("foo", "test");
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
        verify(metadataState, times(1)).getMetadata("foo", "test");
    }

    @Test
    public void getMetadataValueInvalid1() {
        client.addMetadata("foo", "test", true);
        client.getMetadata(null, "test");
        verify(metadataState, times(0)).getMetadata(null, "test");
        assertNotNull(logger.getMsg());
    }

    @Test
    public void getMetadataValueInvalid2() {
        client.addMetadata("foo", "test", true);
        client.getMetadata("foo", null);
        verify(metadataState, times(0)).getMetadata("foo", null);
        assertNotNull(logger.getMsg());
    }

    @Test
    public void leaveBreadcrumbValid() {
        client.leaveBreadcrumb("foo");
        verify(breadcrumbState, times(1)).add(any(Breadcrumb.class));
    }

    @Test
    public void leaveBreadcrumbInvalid() {
        client.leaveBreadcrumb(null);
        verify(breadcrumbState, times(0)).add(any(Breadcrumb.class));
        assertNotNull(logger.getMsg());
    }

    @Test
    public void leaveComplexBreadcrumbValid() {
        HashMap<String, Object> metadata = new HashMap<>();
        client.leaveBreadcrumb("foo", metadata, BreadcrumbType.NAVIGATION);
        verify(breadcrumbState, times(1)).add(any(Breadcrumb.class));
    }

    @Test
    public void leaveComplexBreadcrumbInvalid1() {
        HashMap<String, Object> metadata = new HashMap<>();
        client.leaveBreadcrumb(null, metadata, BreadcrumbType.NAVIGATION);
        verify(breadcrumbState, times(0)).add(any(Breadcrumb.class));
    }

    @Test
    public void leaveComplexBreadcrumbInvalid2() {
        HashMap<String, Object> metadata = new HashMap<>();
        client.leaveBreadcrumb("foo", metadata, null);
        verify(breadcrumbState, times(0)).add(any(Breadcrumb.class));
    }

    @Test
    public void leaveComplexBreadcrumbInvalid3() {
        client.leaveBreadcrumb("foo", null, BreadcrumbType.NAVIGATION);
        verify(breadcrumbState, times(0)).add(any(Breadcrumb.class));
    }

    @Test
    public void addRuntimeVersionInfo() {
        client.addRuntimeVersionInfo("foo", "bar");
        verify(deviceDataCollector, times(1)).addRuntimeVersionInfo("foo", "bar");
    }

    @Test
    public void markLaunchCompleted() {
        client.markLaunchCompleted();
        verify(launchCrashTracker, times(1)).markLaunchCompleted();
    }
}
