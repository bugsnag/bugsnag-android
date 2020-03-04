package com.bugsnag.android;

import static com.bugsnag.android.BugsnagTestUtils.generateConfiguration;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import androidx.annotation.NonNull;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.filters.SmallTest;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Observable;
import java.util.Observer;
import java.util.Set;

@SmallTest
@SuppressWarnings("unchecked")
public class ObserverInterfaceTest {

    private Client client;
    private BugsnagTestObserver observer;

    /**
     * Configures a new AppData for testing accessors + serialisation
     *
     */
    @Before
    public void setUp() {
        Configuration config = generateConfiguration();
        config.setDelivery(BugsnagTestUtils.generateDelivery());
        config.getEnabledErrorTypes().setUnhandledExceptions(false);

        Set<BreadcrumbType> breadcrumbTypes = new HashSet<>();
        breadcrumbTypes.add(BreadcrumbType.LOG);
        breadcrumbTypes.add(BreadcrumbType.MANUAL);
        config.setEnabledBreadcrumbTypes(breadcrumbTypes);
        client = new Client(ApplicationProvider.getApplicationContext(), config);
        observer = new BugsnagTestObserver();
        client.registerObserver(observer);
    }

    @After
    public void tearDown() {
        client.close();
    }

    @Test
    public void testAddMetadataSendsMessage() {
        client.addMetadata("foo", "bar", "baz");
        StateEvent.AddMetadata msg = findMessageInQueue(StateEvent.AddMetadata.class);
        assertEquals("foo", msg.getSection());
        assertEquals("bar", msg.getKey());
        assertEquals("baz", msg.getValue());
    }

    @Test
    public void testAddNullMetadataSendsMessage() {
        client.addMetadata("foo", "bar", "baz");
        client.addMetadata("foo", "bar", null);
        StateEvent.ClearMetadataValue msg = findMessageInQueue(StateEvent.ClearMetadataValue.class);
        assertEquals("foo", msg.getSection());
        assertEquals("bar", msg.getKey());
    }

    @Test
    public void testClearTopLevelTabSendsMessage() {
        client.clearMetadata("axis");
        StateEvent.ClearMetadataSection value
                = findMessageInQueue(StateEvent.ClearMetadataSection.class);
        assertEquals("axis", value.getSection());
    }

    @Test
    public void testClearTabSendsMessage() {
        client.clearMetadata("axis", "foo");
        StateEvent.ClearMetadataValue value
                = findMessageInQueue(StateEvent.ClearMetadataValue.class);
        assertEquals("axis", value.getSection());
        assertEquals("foo", value.getKey());
    }

    @Test
    public void testNotifySendsMessage() {
        client.startSession();
        client.notify(new Exception("ruh roh"));
        assertNotNull(findMessageInQueue(StateEvent.NotifyHandled.class));
    }

    @Test
    public void testStartSessionSendsMessage() {
        client.startSession();
        StateEvent.StartSession sessionInfo = findMessageInQueue(StateEvent.StartSession.class);
        assertNotNull(sessionInfo.getId());
        assertNotNull(sessionInfo.getStartedAt());
        assertEquals(0, sessionInfo.getHandledCount());
        assertEquals(0, sessionInfo.getUnhandledCount());
    }

    @Test
    public void testPauseSessionSendsMessage() {
        client.startSession();
        client.pauseSession();
        assertNotNull(findMessageInQueue(StateEvent.PauseSession.class));
    }

    @Test
    public void testRegisterSessionSendsMessage() {
        client.sessionTracker.registerExistingSession(null, null, null, 0, 1);
        assertNotNull(findMessageInQueue(StateEvent.PauseSession.class));
    }

    @Test
    public void testClientSetContextSendsMessage() {
        client.setContext("Pod Bay");
        StateEvent.UpdateContext msg = findMessageInQueue(StateEvent.UpdateContext.class);
        assertEquals("Pod Bay", msg.getContext());
    }

    @Test
    public void testClientSetUserId() {
        client.setUser("personX", "bip@example.com", "Loblaw");
        StateEvent.UpdateUser idMsg = findMessageInQueue(StateEvent.UpdateUser.class);
        assertEquals("personX", idMsg.getUser().getId());
        assertEquals("bip@example.com", idMsg.getUser().getEmail());
        assertEquals("Loblaw", idMsg.getUser().getName());
    }

    @Test
    public void testLeaveStringBreadcrumbSendsMessage() {
        client.leaveBreadcrumb("Drift 4 units left");
        StateEvent.AddBreadcrumb crumb = findMessageInQueue(StateEvent.AddBreadcrumb.class);
        assertEquals(BreadcrumbType.MANUAL, crumb.getType());
        assertEquals("manual", crumb.getMessage());
        assertEquals(1, crumb.getMetadata().size());
        assertEquals("Drift 4 units left", crumb.getMetadata().get("message"));
    }

    @Test
    public void testLeaveStringBreadcrumbDirectlySendsMessage() {
        Breadcrumb obj = new Breadcrumb("Drift 4 units left", NoopLogger.INSTANCE);
        client.breadcrumbState.add(obj);
        StateEvent.AddBreadcrumb crumb = findMessageInQueue(StateEvent.AddBreadcrumb.class);
        assertEquals(BreadcrumbType.MANUAL, crumb.getType());
        assertEquals("manual", crumb.getMessage());
        assertEquals(1, crumb.getMetadata().size());
        assertEquals("Drift 4 units left", crumb.getMetadata().get("message"));
    }

    @Test
    public void testLeaveBreadcrumbSendsMessage() {
        client.leaveBreadcrumb("Rollback", BreadcrumbType.LOG, new HashMap<String, Object>());
        StateEvent.AddBreadcrumb crumb = findMessageInQueue(StateEvent.AddBreadcrumb.class);
        assertEquals(BreadcrumbType.LOG, crumb.getType());
        assertEquals("Rollback", crumb.getMessage());
        assertEquals(0, crumb.getMetadata().size());
    }

    @NonNull
    private <T extends StateEvent> T findMessageInQueue(Class<T> argClass) {
        for (Object item : observer.observed) {
            if (item.getClass().equals(argClass)) {
                return (T) item;
            }
        }
        throw new RuntimeException("Failed to find StateEvent message " + argClass.getSimpleName());
    }

    static class BugsnagTestObserver implements Observer {
        private final ArrayList<Object> observed;

        BugsnagTestObserver() {
            observed = new ArrayList<>(4);
        }

        @Override
        public void update(Observable observable, Object arg) {
            observed.add(arg);
        }
    }
}
