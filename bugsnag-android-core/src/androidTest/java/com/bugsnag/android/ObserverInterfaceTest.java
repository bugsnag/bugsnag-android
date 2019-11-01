package com.bugsnag.android;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

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
    public void testAddMetadataToClientSendsMessage() {
        client.addMetadata("foo", "bar", "baz");
        List<Object> metadataItem = (List<Object>)findMessageInQueue(
                NativeInterface.MessageType.ADD_METADATA, List.class);
        assertEquals(3, metadataItem.size());
        assertEquals("foo", metadataItem.get(0));
        assertEquals("bar", metadataItem.get(1));
        assertEquals("baz", metadataItem.get(2));
    }

    @Test
    public void testAddNullMetadataToClientSendsMessage() {
        client.addMetadata("foo", "bar", null);
        List<Object> metadataItem = (List<Object>)findMessageInQueue(
                NativeInterface.MessageType.REMOVE_METADATA, List.class);
        assertEquals(2, metadataItem.size());
        assertEquals("foo", metadataItem.get(0));
        assertEquals("bar", metadataItem.get(1));
    }

    @Test
    public void testAddMetadataToMetadataSendsMessage() {
        client.addMetadata("foo", "bar", "baz");
        List<Object> metadataItem = (List<Object>)findMessageInQueue(
                NativeInterface.MessageType.ADD_METADATA, List.class);
        assertEquals(3, metadataItem.size());
        assertEquals("foo", metadataItem.get(0));
        assertEquals("bar", metadataItem.get(1));
        assertEquals("baz", metadataItem.get(2));
    }

    @Test
    public void testClearTabFromClientSendsMessage() {
        client.clearMetadata("axis", null);
        Object value = findMessageInQueue(
                NativeInterface.MessageType.CLEAR_METADATA_TAB, String.class);
        assertEquals("axis", value);
    }

    @Test
    public void testClearTabFromMetadataSendsMessage() {
        client.clearMetadata("axis", null);
        Object value =  findMessageInQueue(
                NativeInterface.MessageType.CLEAR_METADATA_TAB, String.class);
        assertEquals("axis", value);
    }

    @Test
    public void testAddNullMetadataToMetadataSendsMessage() {
        client.addMetadata("foo", "bar", null);
        List<Object> metadataItem = (List<Object>)findMessageInQueue(
                NativeInterface.MessageType.REMOVE_METADATA, List.class);
        assertEquals(2, metadataItem.size());
        assertEquals("foo", metadataItem.get(0));
        assertEquals("bar", metadataItem.get(1));
    }

    @Test
    public void testNotifySendsMessage() {
        client.startSession();
        client.notify(new Exception("ruh roh"));
        Object errorClass = findMessageInQueue(
                NativeInterface.MessageType.NOTIFY_HANDLED, String.class);
        assertEquals("java.lang.Exception", errorClass);
    }

    @Test
    public void testStartSessionSendsMessage() {
        client.startSession();
        List<Object> sessionInfo = (List<Object>)findMessageInQueue(
                NativeInterface.MessageType.START_SESSION, List.class);
        assertEquals(4, sessionInfo.size());
        assertTrue(sessionInfo.get(0) instanceof String);
        assertTrue(sessionInfo.get(1) instanceof String);
        assertTrue(sessionInfo.get(2) instanceof Integer);
        assertTrue(sessionInfo.get(3) instanceof Integer);
    }

    @Test
    public void testPauseSessionSendsmessage() {
        client.startSession();
        client.pauseSession();
        Object msg = findMessageInQueue(NativeInterface.MessageType.PAUSE_SESSION, null);
        assertNull(msg);
    }

    @Test
    public void testClientSetUserId() {
        client.setUserId("personX");
        String value = (String)findMessageInQueue(
                NativeInterface.MessageType.UPDATE_USER_ID, String.class);
        assertEquals("personX", value);
    }

    @Test
    public void testClientSetUserEmail() {
        client.setUserEmail("bip@example.com");
        String value = (String)findMessageInQueue(
                NativeInterface.MessageType.UPDATE_USER_EMAIL, String.class);
        assertEquals("bip@example.com", value);
    }

    @Test
    public void testClientSetUserName() {
        client.setUserName("Loblaw");
        String value = (String)findMessageInQueue(
                NativeInterface.MessageType.UPDATE_USER_NAME, String.class);
        assertEquals("Loblaw", value);
    }

    @Test
    public void testLeaveStringBreadcrumbSendsMessage() {
        client.leaveBreadcrumb("Drift 4 units left");
        Breadcrumb crumb = (Breadcrumb)findMessageInQueue(
                NativeInterface.MessageType.ADD_BREADCRUMB, Breadcrumb.class);
        assertEquals(BreadcrumbType.MANUAL, crumb.getType());
        assertEquals("manual", crumb.getMessage());
        assertEquals(1, crumb.getMetadata().size());
        assertEquals("Drift 4 units left", crumb.getMetadata().get("message"));
    }

    @Test
    public void testLeaveStringBreadcrumbDirectlySendsMessage() {
        client.breadcrumbState.add(new Breadcrumb("Drift 4 units left"));
        Breadcrumb crumb = (Breadcrumb)findMessageInQueue(
                NativeInterface.MessageType.ADD_BREADCRUMB, Breadcrumb.class);
        assertEquals(BreadcrumbType.MANUAL, crumb.getType());
        assertEquals("manual", crumb.getMessage());
        assertEquals(1, crumb.getMetadata().size());
        assertEquals("Drift 4 units left", crumb.getMetadata().get("message"));
    }

    @Test
    public void testClearBreadcrumbsDirectlySendsMessage() {
        client.breadcrumbState.clear();
        findMessageInQueue(NativeInterface.MessageType.CLEAR_BREADCRUMBS, null);
    }

    @Test
    public void testLeaveBreadcrumbSendsMessage() {
        client.leaveBreadcrumb("Rollback", BreadcrumbType.LOG, new HashMap<String, Object>());
        Breadcrumb crumb = (Breadcrumb)findMessageInQueue(
                NativeInterface.MessageType.ADD_BREADCRUMB, Breadcrumb.class);
        assertEquals(BreadcrumbType.LOG, crumb.getType());
        assertEquals("Rollback", crumb.getMessage());
        assertEquals(0, crumb.getMetadata().size());
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
        fail("Failed to find message matching " + type);

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
