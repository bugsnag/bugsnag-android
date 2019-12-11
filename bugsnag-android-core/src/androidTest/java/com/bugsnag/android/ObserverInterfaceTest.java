package com.bugsnag.android;

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
import java.util.Observable;
import java.util.Observer;

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
        Configuration config = new Configuration("some-api-key");
        config.setDelivery(BugsnagTestUtils.generateDelivery());
        config.setAutoDetectErrors(false);
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
        StateEvent.RemoveMetadata msg = findMessageInQueue(StateEvent.RemoveMetadata.class);
        assertEquals("foo", msg.getSection());
        assertEquals("bar", msg.getKey());
    }

    @Test
    public void testClearTopLevelTabSendsMessage() {
        client.clearMetadata("axis");
        StateEvent.ClearMetadataTab value = findMessageInQueue(StateEvent.ClearMetadataTab.class);
        assertEquals("axis", value.getSection());
    }

    @Test
    public void testClearTabSendsMessage() {
        client.clearMetadata("axis", "foo");
        StateEvent.RemoveMetadata value = findMessageInQueue(StateEvent.RemoveMetadata.class);
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
        client.setUserId("personX");
        StateEvent.UpdateUserId msg = findMessageInQueue(StateEvent.UpdateUserId.class);
        assertEquals("personX", msg.getId());
    }

    @Test
    public void testClientSetUserEmail() {
        client.setUserEmail("bip@example.com");
        StateEvent.UpdateUserEmail msg = findMessageInQueue(StateEvent.UpdateUserEmail.class);
        assertEquals("bip@example.com", msg.getEmail());
    }

    @Test
    public void testClientSetUserName() {
        client.setUserName("Loblaw");
        StateEvent.UpdateUserName msg = findMessageInQueue(StateEvent.UpdateUserName.class);
        assertEquals("Loblaw", msg.getName());
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
        client.breadcrumbState.add(new Breadcrumb("Drift 4 units left"));
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
