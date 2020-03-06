package com.bugsnag.android;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.RejectedExecutionException;

class DeliveryDelegate extends BaseObservable {

    final Logger logger;
    private final EventStore eventStore;
    private final ImmutableConfig immutableConfig;
    final BreadcrumbState breadcrumbState;
    private final Notifier notifier;

    DeliveryDelegate(Logger logger, EventStore eventStore,
                     ImmutableConfig immutableConfig, BreadcrumbState breadcrumbState,
                     Notifier notifier) {
        this.logger = logger;
        this.eventStore = eventStore;
        this.immutableConfig = immutableConfig;
        this.breadcrumbState = breadcrumbState;
        this.notifier = notifier;
    }

    void deliver(@NonNull Event event) {
        // Build the eventPayload
        EventPayload eventPayload = new EventPayload(immutableConfig.getApiKey(), event, notifier);
        Session session = event.getSession();

        if (session != null) {
            if (event.isUnhandled()) {
                event.setSession(session.incrementUnhandledAndCopy());
                notifyObservers(StateEvent.NotifyUnhandled.INSTANCE);
            } else {
                event.setSession(session.incrementHandledAndCopy());
                notifyObservers(StateEvent.NotifyHandled.INSTANCE);
            }
        }

        if (event.isUnhandled()) {
            cacheEvent(event, true);
        } else {
            deliverPayloadAsync(event, eventPayload);
        }
    }

    private void deliverPayloadAsync(@NonNull Event event, EventPayload eventPayload) {
        final EventPayload finalEventPayload = eventPayload;
        final Event finalEvent = event;

        // Attempt to send the eventPayload in the background
        try {
            Async.run(new Runnable() {
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
        DeliveryParams deliveryParams = immutableConfig.getErrorApiDeliveryParams();
        Delivery delivery = immutableConfig.getDelivery();
        DeliveryStatus deliveryStatus = delivery.deliver(payload, deliveryParams);

        switch (deliveryStatus) {
            case DELIVERED:
                logger.i("Sent 1 new event to Bugsnag");
                leaveErrorBreadcrumb(event);
                break;
            case UNDELIVERED:
                logger.w("Could not send event(s) to Bugsnag,"
                        + " saving to disk to send later");
                cacheEvent(event, false);
                leaveErrorBreadcrumb(event);
                break;
            case FAILURE:
                logger.w("Problem sending event to Bugsnag");
                break;
            default:
                break;
        }
        return deliveryStatus;
    }

    private void cacheEvent(@NonNull Event event, boolean attemptSend) {
        eventStore.write(event);
        if (attemptSend) {
            eventStore.flushAsync();
        }
    }

    private void leaveErrorBreadcrumb(@NonNull Event event) {
        // Add a breadcrumb for this event occurring
        List<Error> errors = event.getErrors();

        if (errors.size() > 0) {
            String errorClass = errors.get(0).getErrorClass();
            String message = errors.get(0).getErrorMessage();

            Map<String, Object> data = new HashMap<>();
            data.put("errorClass", errorClass);
            data.put("message", message);
            data.put("unhandled", String.valueOf(event.isUnhandled()));
            data.put("severity", event.getSeverity().toString());
            breadcrumbState.add(new Breadcrumb(errorClass,
                    BreadcrumbType.ERROR, data, new Date(), logger));
        }
    }
}
