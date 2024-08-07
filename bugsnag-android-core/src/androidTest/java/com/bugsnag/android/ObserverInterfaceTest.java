package com.bugsnag.android;

import static com.bugsnag.android.BugsnagTestUtils.generateConfiguration;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.bugsnag.android.internal.DateUtils;
import com.bugsnag.android.internal.StateObserver;

import androidx.annotation.NonNull;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.filters.SmallTest;

import org.jetbrains.annotations.NotNull;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
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
        config.addMetadata("foo", "bar", true);
        client = new Client(ApplicationProvider.getApplicationContext(), config);
        observer = new BugsnagTestObserver();
        client.addObserver(observer);
    }

    @After
    public void tearDown() {
        client.close();
    }

    @SuppressWarnings("EmptyCatchBlock")
    @Test
    public void testSyncInitialState() {
        try {
            assertNull(findMessageInQueue(StateEvent.UpdateUser.class));
            fail("UpdateUser message not expected");
        } catch (Throwable ignored) {
        }
        try {
            assertNull(findMessageInQueue(StateEvent.AddMetadata.class));
            fail("AddMetadata message not expected");
        } catch (Throwable ignored) {
        }
        try {
            assertNull(findMessageInQueue(StateEvent.UpdateContext.class));
            fail("UpdateContext message not expected");
        } catch (Throwable ignored) {
        }

        client.syncInitialState();
        assertNotNull(findMessageInQueue(StateEvent.UpdateUser.class));
        assertNotNull(findMessageInQueue(StateEvent.AddMetadata.class));
        assertNotNull(findMessageInQueue(StateEvent.UpdateContext.class));
    }

    @Test
    public void testAddMetadataSendsMessage() {
        client.addMetadata("foo", "bar", "baz");
        StateEvent.AddMetadata msg = findMessageInQueue(StateEvent.AddMetadata.class);
        assertEquals("foo", msg.section);
        assertEquals("bar", msg.key);
        assertEquals("baz", msg.value);
    }

    @Test
    public void testAddNullMetadataSendsMessage() {
        client.addMetadata("foo", "bar", "baz");
        client.addMetadata("foo", "bar", null);
        StateEvent.ClearMetadataValue msg = findMessageInQueue(StateEvent.ClearMetadataValue.class);
        assertEquals("foo", msg.section);
        assertEquals("bar", msg.key);
    }

    @Test
    public void testClearTopLevelTabSendsMessage() {
        client.clearMetadata("axis");
        StateEvent.ClearMetadataSection value
                = findMessageInQueue(StateEvent.ClearMetadataSection.class);
        assertEquals("axis", value.section);
    }

    @Test
    public void testClearTabSendsMessage() {
        client.clearMetadata("axis", "foo");
        StateEvent.ClearMetadataValue value
                = findMessageInQueue(StateEvent.ClearMetadataValue.class);
        assertEquals("axis", value.section);
        assertEquals("foo", value.key);
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
        assertNotNull(sessionInfo.id);
        assertNotNull(sessionInfo.startedAt);
        assertEquals(0, sessionInfo.handledCount);
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
        assertEquals("Pod Bay", msg.context);
    }

    @Test
    public void testClientMarkLaunchCompletedSendsMessage() {
        client.markLaunchCompleted();
        StateEvent.UpdateIsLaunching msg = findMessageInQueue(StateEvent.UpdateIsLaunching.class);
        assertFalse(msg.isLaunching);
    }

    @Test
    public void testClientSetUserId() {
        client.setUser("personX", "bip@example.com", "Loblaw");
        StateEvent.UpdateUser idMsg = findMessageInQueue(StateEvent.UpdateUser.class);
        assertEquals("personX", idMsg.user.getId());
        assertEquals("bip@example.com", idMsg.user.getEmail());
        assertEquals("Loblaw", idMsg.user.getName());
    }

    @Test
    public void testLeaveStringBreadcrumbSendsMessage() {
        client.leaveBreadcrumb("Drift 4 units left");
        StateEvent.AddBreadcrumb crumb = findMessageInQueue(StateEvent.AddBreadcrumb.class);
        assertEquals(BreadcrumbType.MANUAL, crumb.type);
        assertEquals("Drift 4 units left", crumb.message);
        assertTrue(crumb.metadata.isEmpty());
        // DateUtils.fromIso8601 throws an exception on failure, but we also check for nulls
        assertNotNull(DateUtils.fromIso8601(crumb.timestamp));
    }

    @Test
    public void testLeaveStringBreadcrumbDirectlySendsMessage() {
        Breadcrumb obj = new Breadcrumb("Drift 4 units left", NoopLogger.INSTANCE);
        client.breadcrumbState.add(obj);
        StateEvent.AddBreadcrumb crumb = findMessageInQueue(StateEvent.AddBreadcrumb.class);
        assertEquals(BreadcrumbType.MANUAL, crumb.type);
        assertEquals("Drift 4 units left", crumb.message);
        assertTrue(crumb.metadata.isEmpty());
    }

    @Test
    public void testLeaveBreadcrumbSendsMessage() {
        client.leaveBreadcrumb("Rollback", new HashMap<String, Object>(), BreadcrumbType.LOG);
        StateEvent.AddBreadcrumb crumb = findMessageInQueue(StateEvent.AddBreadcrumb.class);
        assertEquals(BreadcrumbType.LOG, crumb.type);
        assertEquals("Rollback", crumb.message);
        assertEquals(0, crumb.metadata.size());
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

    static class BugsnagTestObserver implements StateObserver {
        final ArrayList<Object> observed;

        BugsnagTestObserver() {
            observed = new ArrayList<>(4);
        }

        @Override
        public void onStateChange(@NotNull StateEvent event) {
            observed.add(event);
        }
    }
}
