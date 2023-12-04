package com.bugsnag.android;

import static com.bugsnag.android.SeverityReason.REASON_PROMISE_REJECTION;

import com.bugsnag.android.internal.BackgroundTaskService;
import com.bugsnag.android.internal.ImmutableConfig;
import com.bugsnag.android.internal.TaskType;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;

import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;

class DeliveryDelegate extends BaseObservable {

    @VisibleForTesting
    static long DELIVERY_TIMEOUT = 3000L;

    final Logger logger;
    private final EventStore eventStore;
    private final ImmutableConfig immutableConfig;
    private final Notifier notifier;
    private final CallbackState callbackState;
    final BackgroundTaskService backgroundTaskService;

    DeliveryDelegate(Logger logger,
                     EventStore eventStore,
                     ImmutableConfig immutableConfig,
                     CallbackState callbackState,
                     Notifier notifier,
                     BackgroundTaskService backgroundTaskService) {
        this.logger = logger;
        this.eventStore = eventStore;
        this.immutableConfig = immutableConfig;
        this.callbackState = callbackState;
        this.notifier = notifier;
        this.backgroundTaskService = backgroundTaskService;
    }

    void deliver(@NonNull Event event) {
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

        if (event.getOriginalUnhandled()) {
            // should only send unhandled errors if they don't terminate the process (i.e. ANRs)
            String severityReasonType = event.getSeverityReasonType();
            boolean promiseRejection = REASON_PROMISE_REJECTION.equals(severityReasonType);
            boolean anr = event.isAnr(event);
            if (anr || promiseRejection) {
                cacheEvent(event, true);
            } else if (immutableConfig.getAttemptDeliveryOnCrash()) {
                cacheAndSendSynchronously(event);
            } else {
                cacheEvent(event, false);
            }
        } else if (callbackState.runOnSendTasks(event, logger)) {
            // Build the eventPayload
            String apiKey = event.getApiKey();
            EventPayload eventPayload = new EventPayload(apiKey, event, notifier, immutableConfig);
            deliverPayloadAsync(event, eventPayload);
        }
    }

    private void deliverPayloadAsync(@NonNull Event event, EventPayload eventPayload) {
        final EventPayload finalEventPayload = eventPayload;
        final Event finalEvent = event;

        // Attempt to send the eventPayload in the background
        try {
            backgroundTaskService.submitTask(TaskType.ERROR_REQUEST, new Runnable() {
                @Override
                public void run() {
                    deliverPayloadInternal(finalEventPayload, finalEvent);
                }
            });
        } catch (RejectedExecutionException exception) {
            cacheEvent(event, false);
            logger.w("Exceeded max queue count, saving to disk to send later");
        }
    }

    @VisibleForTesting
    DeliveryStatus deliverPayloadInternal(@NonNull EventPayload payload, @NonNull Event event) {
        logger.d("DeliveryDelegate#deliverPayloadInternal() - attempting event delivery");
        DeliveryParams deliveryParams = immutableConfig.getErrorApiDeliveryParams(payload);
        Delivery delivery = immutableConfig.getDelivery();
        DeliveryStatus deliveryStatus = delivery.deliver(payload, deliveryParams);

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
        Future<String> task = eventStore.writeAndDeliver(event);

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
        eventStore.write(event);
        if (attemptSend) {
            eventStore.flushAsync();
        }
    }
}
