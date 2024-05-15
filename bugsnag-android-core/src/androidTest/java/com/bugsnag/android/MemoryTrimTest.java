package com.bugsnag.android;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bugsnag.android.ObserverInterfaceTest.BugsnagTestObserver;

import android.content.ComponentCallbacks;
import android.content.Context;
import androidx.test.core.app.ApplicationProvider;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * Heavily mocked test to ensure that onLowMemory events are distributed to Client Observers
 */
@RunWith(MockitoJUnitRunner.class)
public class MemoryTrimTest {

    @Spy
    Context context = ApplicationProvider.getApplicationContext();

    @Captor
    ArgumentCaptor<ComponentCallbacks> componentCallbacksCaptor;

    @Test
    public void onLowMemoryEvent() {
        when(context.getApplicationContext()).thenReturn(context);
        doNothing().when(context).registerComponentCallbacks(any());
        Client client = new Client(context, BugsnagTestUtils.generateConfiguration());

        // block until observer is registered
        client.bgTaskService.shutdown();

        // capture the registered ComponentCallbacks
        verify(context, times(1)).registerComponentCallbacks(componentCallbacksCaptor.capture());

        BugsnagTestObserver observer = new BugsnagTestObserver();
        client.addObserver(observer);

        ComponentCallbacks callbacks = componentCallbacksCaptor.getValue();
        callbacks.onLowMemory();

        assertEquals(1, observer.observed.size());
        Object observedEvent = observer.observed.get(0);

        assertTrue(
                "observed event should be UpdateMemoryTrimEvent",
                observedEvent instanceof StateEvent.UpdateMemoryTrimEvent
        );

        assertTrue(
                "observed event should be marked isLowMemory",
                ((StateEvent.UpdateMemoryTrimEvent) observedEvent).isLowMemory
        );
    }

}
