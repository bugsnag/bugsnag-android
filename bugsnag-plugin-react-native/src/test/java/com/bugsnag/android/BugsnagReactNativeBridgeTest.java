package com.bugsnag.android;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import kotlin.Unit;
import kotlin.jvm.functions.Function1;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@RunWith(MockitoJUnitRunner.class)
public class BugsnagReactNativeBridgeTest {

    private Map<String, Object> metadata;

    @Mock
    Client client;

    /**
     * Constructs a client which returns a dummy metadata value
     */
    @Before
    public void setUp() {
        Map<String, Boolean> bar = Collections.singletonMap("Bar", true);
        metadata = Collections.<String, Object>singletonMap("foo", bar);
        Mockito.when(client.getMetadata()).thenReturn(metadata);
    }

    @Test
    public void userUpdate() {
        MessageEventCb cb = new MessageEventCb();
        BugsnagReactNativeBridge bridge = new BugsnagReactNativeBridge(client, cb);

        User user = new User("123", "joe@example.com", "Joe Bloggs");
        bridge.onStateChange(new StateEvent.UpdateUser(user));
        assertNotNull(cb.event);
        assertEquals("UserUpdate", cb.event.getType());

        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) cb.event.getData();
        assertEquals("123", data.get("id"));
        assertEquals("joe@example.com", data.get("email"));
        assertEquals("Joe Bloggs", data.get("name"));
    }

    @Test
    public void contextUpdate() {
        MessageEventCb cb = new MessageEventCb();
        BugsnagReactNativeBridge bridge = new BugsnagReactNativeBridge(client, cb);

        bridge.onStateChange(new StateEvent.UpdateContext("Foo"));
        assertNotNull(cb.event);
        assertEquals("ContextUpdate", cb.event.getType());
        assertEquals("Foo", cb.event.getData());
    }

    @Test
    public void addMetadataUpdate() {
        MessageEventCb cb = new MessageEventCb();
        BugsnagReactNativeBridge bridge = new BugsnagReactNativeBridge(client, cb);

        StateEvent.AddMetadata arg = new StateEvent.AddMetadata("foo", "bar", true);
        bridge.onStateChange(arg);
        assertNotNull(cb.event);
        assertEquals("MetadataUpdate", cb.event.getType());
        assertEquals(metadata, cb.event.getData());
    }

    @Test
    public void clearMetadataSectionUpdate() {
        MessageEventCb cb = new MessageEventCb();
        BugsnagReactNativeBridge bridge = new BugsnagReactNativeBridge(client, cb);

        StateEvent.ClearMetadataSection arg = new StateEvent.ClearMetadataSection("baz");
        bridge.onStateChange(arg);
        assertNotNull(cb.event);
        assertEquals("MetadataUpdate", cb.event.getType());
        assertEquals(metadata, cb.event.getData());
    }

    @Test
    public void clearMetadataValueUpdate() {
        MessageEventCb cb = new MessageEventCb();
        BugsnagReactNativeBridge bridge = new BugsnagReactNativeBridge(client, cb);

        StateEvent.ClearMetadataValue arg = new StateEvent.ClearMetadataValue("baz", "wham");
        bridge.onStateChange(arg);
        assertNotNull(cb.event);
        assertEquals("MetadataUpdate", cb.event.getType());
        assertEquals(metadata, cb.event.getData());
    }

    @Test
    public void addFeatureFlag() {
        MessageEventCb cb = new MessageEventCb();
        BugsnagReactNativeBridge bridge = new BugsnagReactNativeBridge(client, cb);

        StateEvent.AddFeatureFlag arg = new StateEvent.AddFeatureFlag("feature", "var");
        bridge.onStateChange(arg);
        assertNotNull(cb.event);
        assertEquals("AddFeatureFlag", cb.event.getType());

        Map<String, Object> data = new HashMap<>();
        data.put("name", "feature");
        data.put("variant", "var");
        assertEquals(data, cb.event.getData());
    }

    @Test
    public void clearFeatureFlag() {
        MessageEventCb cb = new MessageEventCb();
        BugsnagReactNativeBridge bridge = new BugsnagReactNativeBridge(client, cb);

        StateEvent.ClearFeatureFlag arg = new StateEvent.ClearFeatureFlag("feature");
        bridge.onStateChange(arg);
        assertNotNull(cb.event);
        assertEquals("ClearFeatureFlag", cb.event.getType());
        assertEquals(Collections.singletonMap("name", "feature"), cb.event.getData());
    }

    @Test
    public void clearAllFeatureFlags() {
        MessageEventCb cb = new MessageEventCb();
        BugsnagReactNativeBridge bridge = new BugsnagReactNativeBridge(client, cb);

        StateEvent.ClearFeatureFlags arg = StateEvent.ClearFeatureFlags.INSTANCE;
        bridge.onStateChange(arg);
        assertNotNull(cb.event);
        assertEquals("ClearFeatureFlag", cb.event.getType());
        assertNull(cb.event.getData());
    }

    static class MessageEventCb implements Function1<MessageEvent, Unit> {
        MessageEvent event;

        @Override
        public Unit invoke(MessageEvent messageEvent) {
            this.event = messageEvent;
            return null;
        }
    }
}
