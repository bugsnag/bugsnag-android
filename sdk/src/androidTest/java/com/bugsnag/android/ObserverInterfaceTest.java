package com.bugsnag.android;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import android.support.test.InstrumentationRegistry;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.HashMap;
import java.util.Observer;
import java.util.Observable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;


@RunWith(AndroidJUnit4.class)
@SmallTest
@SuppressWarnings("unchecked")
public class ObserverInterfaceTest {
    private Configuration config;
    private Client client;
    private BugsnagTestObserver observer;
    private CountDownLatch lock = new CountDownLatch(1);

    /**
     * Configures a new AppData for testing accessors + serialisation
     *
     * @throws Exception if setup failed
     */
    @Before
    public void setUp() throws Exception {
        config = new Configuration("some-api-key");
        config.setDelivery(new Delivery() {
            @Override
            public void deliver(SessionTrackingPayload payload, Configuration config) throws DeliveryFailureException {
            }

            @Override
            public void deliver(Report report, Configuration config) throws DeliveryFailureException {
            }
        });
        client = new Client(InstrumentationRegistry.getContext(), config);
        client.disableExceptionHandler();
        observer = new BugsnagTestObserver();
        client.addObserver(observer);
    }

    @Test
    public void testUpdateMetadataFromClientSendsMessage() {
        MetaData metadata = new MetaData(new HashMap<String, Object>());
        metadata.addToTab("foo", "bar", "baz");
        client.setMetaData(metadata);
        Object value = findMessageInQueue(NativeInterface.MessageType.UPDATE_METADATA, MetaData.class);
        assertEquals(metadata, value);
    }

    @Test
    public void testUpdateMetadataFromConfigSendsMessage() {
        MetaData metadata = new MetaData(new HashMap<String, Object>());
        metadata.addToTab("foo", "bar", "baz");
        client.getConfig().setMetaData(metadata);
        Object value = findMessageInQueue(NativeInterface.MessageType.UPDATE_METADATA, MetaData.class);
        assertEquals(metadata, value);
    }

    @Test
    public void testAddMetadataToClientSendsMessage() {
        client.addToTab("foo", "bar", "baz");
        List<Object> metadataItem = (List<Object>)findMessageInQueue(NativeInterface.MessageType.ADD_METADATA, List.class);
        assertEquals(3, metadataItem.size());
        assertEquals("foo", metadataItem.get(0));
        assertEquals("bar", metadataItem.get(1));
        assertEquals("baz", metadataItem.get(2));
    }

    @Test
    public void testAddNullMetadataToClientSendsMessage() {
        client.addToTab("foo", "bar", null);
        List<Object> metadataItem = (List<Object>)findMessageInQueue(NativeInterface.MessageType.REMOVE_METADATA, List.class);
        assertEquals(2, metadataItem.size());
        assertEquals("foo", metadataItem.get(0));
        assertEquals("bar", metadataItem.get(1));
    }

    @Test
    public void testAddMetadataToMetaDataSendsMessage() {
        client.getMetaData().addToTab("foo", "bar", "baz");
        List<Object> metadataItem = (List<Object>)findMessageInQueue(NativeInterface.MessageType.ADD_METADATA, List.class);
        assertEquals(3, metadataItem.size());
        assertEquals("foo", metadataItem.get(0));
        assertEquals("bar", metadataItem.get(1));
        assertEquals("baz", metadataItem.get(2));
    }

    @Test
    public void testClearTabFromClientSendsMessage() {
        client.clearTab("axis");
        Object value = findMessageInQueue(NativeInterface.MessageType.CLEAR_METADATA_TAB, String.class);
        assertEquals("axis", value);
    }

    @Test
    public void testClearTabFromMetaDataSendsMessage() {
        client.getMetaData().clearTab("axis");
        Object value =  findMessageInQueue(NativeInterface.MessageType.CLEAR_METADATA_TAB, String.class);
        assertEquals("axis", value);
    }

    @Test
    public void testAddNullMetadataToMetaDataSendsMessage() {
        client.getMetaData().addToTab("foo", "bar", null);
        List<Object> metadataItem = (List<Object>)findMessageInQueue(NativeInterface.MessageType.REMOVE_METADATA, List.class);
        assertEquals(2, metadataItem.size());
        assertEquals("foo", metadataItem.get(0));
        assertEquals("bar", metadataItem.get(1));
    }

    @Test
    public void testClientSetReleaseStageSendsMessage() {
        client.setReleaseStage("prod-2");
        Object value = findMessageInQueue(NativeInterface.MessageType.UPDATE_RELEASE_STAGE, String.class);
        assertEquals("prod-2", value);
    }

    @Test
    public void testConfigSetReleaseStageSendsMessage() {
        client.getConfig().setReleaseStage("prod-2");
        Object value = findMessageInQueue(NativeInterface.MessageType.UPDATE_RELEASE_STAGE, String.class);
        assertEquals("prod-2", value);
    }


    @Test
    public void testNotifySendsMessage() throws InterruptedException {
        client.startSession();
        client.notify(new Exception("ruh roh"));
        Object errorClass = findMessageInQueue(NativeInterface.MessageType.NOTIFY_HANDLED, String.class);
        assertEquals("java.lang.Exception", errorClass);
    }

    @Test
    public void testStartSessionSendsMessage() throws InterruptedException {
        client.startSession();
        findMessageInQueue(NativeInterface.MessageType.START_SESSION, Session.class);
    }

    @Test
    public void testClientSetBuildUUIDSendsMessage() {
        client.setBuildUUID("234423-a");
        Object value = findMessageInQueue(NativeInterface.MessageType.UPDATE_BUILD_UUID, String.class);
        assertEquals("234423-a", value);
    }

    @Test
    public void testConfigSetBuildUUIDSendsMessage() {
        client.getConfig().setBuildUUID("234423-a");
        Object value = findMessageInQueue(NativeInterface.MessageType.UPDATE_BUILD_UUID, String.class);
        assertEquals("234423-a", value);
    }

    @Test
    public void testClientSetAppVersionSendsMessage() {
        client.setAppVersion("300.0.1x");
        Object value = findMessageInQueue(NativeInterface.MessageType.UPDATE_APP_VERSION, String.class);
        assertEquals("300.0.1x", value);
    }

    @Test
    public void testConfigSetAppVersionSendsMessage() {
        client.getConfig().setAppVersion("300.0.1x");
        Object value = findMessageInQueue(NativeInterface.MessageType.UPDATE_APP_VERSION, String.class);
        assertEquals("300.0.1x", value);
    }

    @Test
    public void testClientSetContextSendsMessage() {
        client.setContext("Pod Bay");
        String context = (String)findMessageInQueue(NativeInterface.MessageType.UPDATE_CONTEXT, String.class);
        assertEquals("Pod Bay", context);
    }

    @Test
    public void testConfigSetContextSendsMessage() {
        client.getConfig().setContext("Pod Bay");
        String context = (String)findMessageInQueue(NativeInterface.MessageType.UPDATE_CONTEXT, String.class);
        assertEquals("Pod Bay", context);
    }

    @Test
    public void testClientClearUserSendsMessage() {
        client.clearUser();
        findMessageInQueue(NativeInterface.MessageType.UPDATE_USER_ID, String.class); // resets to device ID
        findMessageInQueue(NativeInterface.MessageType.UPDATE_USER_EMAIL, null);
        findMessageInQueue(NativeInterface.MessageType.UPDATE_USER_NAME, null);
    }

    @Test
    public void testLeaveStringBreadcrumbSendsMessage() {
        client.leaveBreadcrumb("Drift 4 units left");
        Breadcrumb crumb = (Breadcrumb)findMessageInQueue(NativeInterface.MessageType.ADD_BREADCRUMB, Breadcrumb.class);
        assertEquals(BreadcrumbType.MANUAL, crumb.getType());
        assertEquals("manual", crumb.getName());
        assertEquals(1, crumb.getMetadata().size());
        assertEquals("Drift 4 units left", crumb.getMetadata().get("message"));
    }

    @Test
    public void testLeaveStringBreadcrumbDirectlySendsMessage() {
        client.breadcrumbs.add(new Breadcrumb("Drift 4 units left"));
        Breadcrumb crumb = (Breadcrumb)findMessageInQueue(NativeInterface.MessageType.ADD_BREADCRUMB, Breadcrumb.class);
        assertEquals(BreadcrumbType.MANUAL, crumb.getType());
        assertEquals("manual", crumb.getName());
        assertEquals(1, crumb.getMetadata().size());
        assertEquals("Drift 4 units left", crumb.getMetadata().get("message"));
    }

    @Test
    public void testClearBreadcrumbsSendsMessage() {
        client.clearBreadcrumbs();
        findMessageInQueue(NativeInterface.MessageType.CLEAR_BREADCRUMBS, null);
    }

    @Test
    public void testClearBreadcrumbsDirectlySendsMessage() {
        client.breadcrumbs.clear();
        findMessageInQueue(NativeInterface.MessageType.CLEAR_BREADCRUMBS, null);
    }

    @Test
    public void testLeaveBreadcrumbSendsMessage() {
        client.leaveBreadcrumb("Rollback", BreadcrumbType.LOG, new HashMap<String, String>());
        Breadcrumb crumb = (Breadcrumb)findMessageInQueue(NativeInterface.MessageType.ADD_BREADCRUMB, Breadcrumb.class);
        assertEquals(BreadcrumbType.LOG, crumb.getType());
        assertEquals("Rollback", crumb.getName());
        assertEquals(0, crumb.getMetadata().size());
    }

    Object findMessageInQueue(NativeInterface.MessageType type, Class<?> argClass) {
        for (Object item : observer.observed) {
            if (item instanceof  NativeInterface.Message) {
                if (argClass == null) {
                    if (((NativeInterface.Message)item).value == null)
                        return null;
                } else if (argClass.isInstance(((NativeInterface.Message)item).value)) {
                    return ((NativeInterface.Message)item).value;
                }
            }
        }
        assertFalse("Failed to find message matching " + type, false);
        return null;
    }

    @SuppressWarnings("unchecked")
    static class BugsnagTestObserver implements Observer {
        private final ArrayList<Object> observed;
        BugsnagTestObserver() {
            observed = new ArrayList<>(4);
        }

        public void update(Observable o, Object arg) {
            observed.add(arg);
        }
    }
}
