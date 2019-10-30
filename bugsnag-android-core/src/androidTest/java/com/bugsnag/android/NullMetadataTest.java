package com.bugsnag.android;

import static com.bugsnag.android.BugsnagTestUtils.generateClient;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import androidx.annotation.NonNull;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Ensures that setting metadata to null doesn't result in NPEs
 * <p>
 * See https://github.com/bugsnag/bugsnag-android/issues/194
 */
@SuppressWarnings("unchecked")
public class NullMetadataTest {

    private static final String TAB_KEY = "tab";

    private ImmutableConfig config;
    private Throwable throwable;
    private Client client;

    /**
     * Generates a bugsnag client with a NOP error api client
     *
     * @throws Exception if initialisation failed
     */
    @Before
    public void setUp() throws Exception {
        client = generateClient(new Configuration("api-key"));
        config = client.immutableConfig;
        throwable = new RuntimeException("Test");
    }

    @After
    public void tearDown() {
        client.close();
    }

    @Test
    public void testErrorDefaultMetadata() throws Exception {
        HandledState handledState = HandledState.newInstance(HandledState.REASON_HANDLED_EXCEPTION);
        Event event = new Event(throwable, config, handledState);
        validateDefaultMetadata(event);
    }

    @Test
    public void testNotify() throws Exception {
        client.addOnError(new OnError() {
            @Override
            public boolean run(@NonNull Event event) {
                validateDefaultMetadata(event);
                return false;
            }
        });
        HandledState handledState = HandledState.newInstance(HandledState.REASON_HANDLED_EXCEPTION);
        Event event = new Event(throwable, config, handledState);
        client.notifyInternal(event, null);
    }

    private void validateDefaultMetadata(MetadataAware metadata) {
        assertNotNull(metadata);
        assertNull(metadata.getMetadata(TAB_KEY, null));
        metadata.addMetadata(TAB_KEY, "test", "data");
        assertEquals("data", metadata.getMetadata(TAB_KEY, "test"));
    }

}
