package com.bugsnag.android;

import com.bugsnag.android.internal.BackgroundTaskService;
import com.bugsnag.android.internal.DeliveryPipeline;
import com.bugsnag.android.internal.ImmutableConfig;
import com.bugsnag.android.internal.TaskType;
import com.bugsnag.android.internal.dag.Provider;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;

public class DeliveryDelegate extends BaseObservable {

    @VisibleForTesting
    static long DELIVERY_TIMEOUT = 3000L;

    final Logger logger;
    private final Provider<EventStore> eventStore;
    private final ImmutableConfig immutableConfig;
    private final Notifier notifier;
    private final DeliveryPipeline deliveryPipeline;
    final BackgroundTaskService backgroundTaskService;

    /**
     * Creates a delegate which delivers events and updates persisted state.
     */
    public DeliveryDelegate(@NonNull Logger logger,
                            @NonNull Provider<EventStore> eventStore,
                            @NonNull ImmutableConfig immutableConfig,
                            @NonNull DeliveryPipeline deliveryPipeline,
                            @NonNull Notifier notifier,
                            @NonNull BackgroundTaskService backgroundTaskService) {
        this.logger = logger;
        this.eventStore = eventStore;
        this.immutableConfig = immutableConfig;
        this.deliveryPipeline = deliveryPipeline;
        this.notifier = notifier;
        this.backgroundTaskService = backgroundTaskService;
    }

    /**
     * Delivers the given event using the appropriate delivery strategy.
     */
    public void deliver(@NonNull Event event) {
        logger.d("DeliveryDelegate#deliver() - event being stored/delivered by Client");
        Session session = event.getSession();

        if (session != null) {
            if (event.isUnhandled()) {
                event.setSession(session.incrementUnhandledAndCopy());
                updateState(StateEvent.NotifyUnhandled.INSTANCE);
            } else {
                event.setSession(session.incrementHandledAndCopy());
                updateState(StateEvent.NotifyHandled.INSTANCE);
            }
        }

        switch (event.getDeliveryStrategy()) {
            case STORE_AND_SEND:
                cacheAndSendSynchronously(event);
                break;
            case STORE_ONLY:
                cacheEvent(event, false);
                break;
            case SEND_IMMEDIATELY:
                deliverPayloadAsync(
                    createEventPayload(event)
                );
                break;
            case STORE_AND_FLUSH:
            default:
                cacheEvent(event, true);
                break;
        }
    }

    private void deliverPayloadAsync(final EventPayload eventPayload) {
        // Attempt to send the eventPayload in the background
        try {
            backgroundTaskService.submitTask(TaskType.ERROR_REQUEST,
                () -> deliverPayloadInternal(eventPayload));
        } catch (RejectedExecutionException exception) {
            Event event = eventPayload.getEvent();
            if (event != null) {
                cacheEvent(event, false);
            }
            logger.w("Exceeded max queue count, saving to disk to send later");
        }
    }

    /**
     * Attempts to deliver a payload via the configured delivery pipeline.
     */
    @VisibleForTesting
    @Nullable
    public DeliveryStatus deliverPayloadInternal(@NonNull EventPayload payload) {
        logger.d("DeliveryDelegate#deliverPayloadInternal() - attempting event delivery");
        Event event = payload.getEvent();
        if (event == null) {
            return null;
        }
        DeliveryStatus deliveryStatus = deliveryPipeline.deliverEventPayload(payload);
        if (deliveryStatus == null) {
            return null;
        }

        switch (deliveryStatus) {
            case DELIVERED:
                logger.i("Sent 1 new event to Bugsnag");
                break;
            case UNDELIVERED:
                logger.w("Could not send event(s) to Bugsnag,"
                        + " saving to disk to send later");
                cacheEvent(event, false);
                break;
            case FAILURE:
                logger.w("Problem sending event to Bugsnag");
                break;
            default:
                break;
        }
        return deliveryStatus;
    }

    private void cacheAndSendSynchronously(@NonNull Event event) {
        long cutoffTime = System.currentTimeMillis() + DELIVERY_TIMEOUT;
        Future<String> task = eventStore().writeAndDeliver(event);

        long timeout = cutoffTime - System.currentTimeMillis();
        if (task != null && timeout > 0) {
            try {
                task.get(timeout, TimeUnit.MILLISECONDS);
            } catch (Exception ex) {
                logger.w("failed to immediately deliver event", ex);
            }

            if (!task.isDone()) {
                task.cancel(true);
            }
        }
    }

    private void cacheEvent(@NonNull Event event, boolean attemptSend) {
        eventStore().write(event);
        if (attemptSend) {
            eventStore().flushAsync();
        }
    }

    private EventPayload createEventPayload(@NonNull Event event) {
        return new EventPayload(event.getApiKey(), event, notifier, immutableConfig);
    }

    private EventStore eventStore() {
        return eventStore.get();
    }
}
