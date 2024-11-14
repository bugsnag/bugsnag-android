package com.bugsnag.android;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bugsnag.android.internal.ImmutableConfig;
import com.bugsnag.android.internal.InternalMetrics;
import com.bugsnag.android.internal.StateObserver;
import com.bugsnag.android.internal.dag.ValueProvider;

import android.content.Context;

import androidx.annotation.NonNull;

import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.ArrayList;
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
    FeatureFlagState featureFlagState;

    @Mock
    ClientObservable clientObservable;

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
    SystemBroadcastReceiver systemBroadcastReceiver;

    @Mock
    SessionTracker sessionTracker;

    @Mock
    Connectivity connectivity;

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

    @Mock
    ExceptionHandler exceptionHandler;

    @Mock
    Notifier notifier;

    @Mock
    InternalMetrics internalMetrics;

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
                new ValueProvider<UserState>(userState),
                featureFlagState,
                clientObservable,
                appContext,
                deviceDataCollector,
                appDataCollector,
                breadcrumbState,
                eventStore,
                systemBroadcastReceiver,
                sessionTracker,
                connectivity,
                logger,
                deliveryDelegate,
                lastRunInfoStore,
                launchCrashTracker,
                exceptionHandler,
                notifier
        );

        // required fields for generating an event
        when(metadataState.getMetadata()).thenReturn(new Metadata());
        when(featureFlagState.getFeatureFlags()).thenReturn(new FeatureFlags());
        when(immutableConfig.getLogger()).thenReturn(logger);
        when(immutableConfig.getApiKey()).thenReturn("test-apiKey");
        when(immutableConfig.getSendThreads()).thenReturn(ThreadSendPolicy.ALWAYS);

        when(deviceDataCollector.generateDeviceWithState(anyLong())).thenReturn(device);
        when(deviceDataCollector.getDeviceMetadata()).thenReturn(new HashMap<String, Object>());
        when(appDataCollector.generateAppWithState()).thenReturn(app);
        when(appDataCollector.getAppDataMetadata()).thenReturn(new HashMap<String, Object>());

        when(breadcrumbState.copy()).thenReturn(new ArrayList<Breadcrumb>());
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
        verify(contextState, times(1)).setManualContext("foo");
        client.setContext(null);
        verify(contextState, times(1)).setManualContext(null);
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


    @Test
    public void registerObserver() {
        StateObserver observer = new StateObserver() {
            @Override
            public void onStateChange(@NotNull StateEvent event) {
            }
        };
        client.addObserver(observer);

        verify(metadataState, times(1)).addObserver(observer);
        verify(breadcrumbState, times(1)).addObserver(observer);
        verify(sessionTracker, times(1)).addObserver(observer);
        verify(clientObservable, times(1)).addObserver(observer);
        verify(userState, times(1)).addObserver(observer);
        verify(contextState, times(1)).addObserver(observer);
        verify(deliveryDelegate, times(1)).addObserver(observer);
        verify(launchCrashTracker, times(1)).addObserver(observer);
    }

    @Test
    public void unregisterObserver() {
        StateObserver observer = new StateObserver() {
            @Override
            public void onStateChange(@NotNull StateEvent event) {
            }
        };
        client.removeObserver(observer);

        verify(metadataState, times(1)).removeObserver(observer);
        verify(breadcrumbState, times(1)).removeObserver(observer);
        verify(sessionTracker, times(1)).removeObserver(observer);
        verify(clientObservable, times(1)).removeObserver(observer);
        verify(userState, times(1)).removeObserver(observer);
        verify(contextState, times(1)).removeObserver(observer);
        verify(deliveryDelegate, times(1)).removeObserver(observer);
        verify(launchCrashTracker, times(1)).removeObserver(observer);
    }

    @Test
    public void notifyLeavesErrorBreadcrumb() {
        client.notify(new RuntimeException("Whoops!"));

        ArgumentCaptor<Breadcrumb> breadcrumbCaptor = ArgumentCaptor.forClass(Breadcrumb.class);
        verify(breadcrumbState).add(breadcrumbCaptor.capture());
        Breadcrumb breadcrumb = breadcrumbCaptor.getValue();

        assertEquals(BreadcrumbType.ERROR, breadcrumb.getType());
        assertEquals("java.lang.RuntimeException", breadcrumb.getMessage());
        assertEquals("java.lang.RuntimeException", breadcrumb.getMetadata().get("errorClass"));
        assertEquals("Whoops!", breadcrumb.getMetadata().get("message"));
        assertEquals("false", breadcrumb.getMetadata().get("unhandled"));
        assertEquals("WARNING", breadcrumb.getMetadata().get("severity"));
    }

    @Test
    public void notifyUnhandledLeavesErrorBreadcrumb() {
        client.notifyUnhandledException(
                new RuntimeException("Whoops!"),
                new Metadata(),
                SeverityReason.REASON_UNHANDLED_EXCEPTION,
                null
        );

        ArgumentCaptor<Breadcrumb> breadcrumbCaptor = ArgumentCaptor.forClass(Breadcrumb.class);
        verify(breadcrumbState).add(breadcrumbCaptor.capture());
        Breadcrumb breadcrumb = breadcrumbCaptor.getValue();

        assertEquals(BreadcrumbType.ERROR, breadcrumb.getType());
        assertEquals("java.lang.RuntimeException", breadcrumb.getMessage());
        assertEquals("java.lang.RuntimeException", breadcrumb.getMetadata().get("errorClass"));
        assertEquals("Whoops!", breadcrumb.getMetadata().get("message"));
        assertEquals("true", breadcrumb.getMetadata().get("unhandled"));
        assertEquals("ERROR", breadcrumb.getMetadata().get("severity"));
    }

}
