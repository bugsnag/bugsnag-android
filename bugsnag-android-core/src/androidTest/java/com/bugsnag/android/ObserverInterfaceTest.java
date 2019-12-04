package com.bugsnag.android;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import androidx.annotation.NonNull;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.filters.SmallTest;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Observable;
import java.util.Observer;

@SmallTest
@SuppressWarnings("unchecked")
public class ObserverInterfaceTest {

    private Configuration config;
    private Client client;
    private BugsnagTestObserver observer;

    /**
     * Configures a new AppData for testing accessors + serialisation
     *
     * @throws Exception if setup failed
     */
    @Before
    public void setUp() throws Exception {
        config = new Configuration("some-api-key");
        config.setDelivery(BugsnagTestUtils.generateDelivery());
        config.setAutoDetectErrors(false);
        client = new Client(ApplicationProvider.getApplicationContext(), config);
        observer = new BugsnagTestObserver();
        client.addObserver(observer);
    }

    @After
    public void tearDown() {
        client.close();
    }

    @Test
    public void testAddMetadataToClientSendsMessage() {
        client.addMetadata("foo", "bar", "baz");
        StateEvent.AddMetadata msg = findMessageInQueue(StateEvent.AddMetadata.class);
        assertEquals("foo", msg.getSection());
        assertEquals("bar", msg.getKey());
        assertEquals("baz", msg.getValue());
    }

    @Test
    public void testAddNullMetadataToClientSendsMessage() {
        client.addMetadata("foo", "bar", null);
        StateEvent.RemoveMetadata msg = findMessageInQueue(StateEvent.RemoveMetadata.class);
        assertEquals("foo", msg.getSection());
        assertEquals("bar", msg.getKey());
    }

    @Test
    public void testAddMetadataToMetadataSendsMessage() {
        client.addMetadata("foo", "bar", "baz");
        StateEvent.AddMetadata metadataItem = findMessageInQueue(StateEvent.AddMetadata.class);
        assertEquals("foo", metadataItem.getSection());
        assertEquals("bar", metadataItem.getKey());
        assertEquals("baz", metadataItem.getValue());
    }

    @Test
    public void testClearTabFromClientSendsMessage() {
        client.clearMetadata("axis");
        StateEvent.ClearMetadataTab value = findMessageInQueue(StateEvent.ClearMetadataTab.class);
        assertEquals("axis", value.getSection());
    }

    @Test
    public void testAddNullMetadataToMetadataSendsMessage() {
        client.addMetadata("foo", "bar", null);
        StateEvent.RemoveMetadata msg = findMessageInQueue(StateEvent.RemoveMetadata.class);
        assertEquals("foo", msg.getSection());
        assertEquals("bar", msg.getKey());
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

    private Object findMessageInQueue(NativeInterface.MessageType type, Class<?> argClass) {
        for (Object item : observer.observed) {
            if (item instanceof  NativeInterface.Message) {
                NativeInterface.Message message = (NativeInterface.Message)item;
                if (message.type != type) {
                    continue;
                }
                if (argClass == null) {
                    if (((NativeInterface.Message)item).value == null) {
                        return null;
                    }
                } else if (argClass.isInstance(message.value)) {
                    return message.value;
                }
            }
        }
        assertTrue("Failed to find message matching " + type, false);

        return null;
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
