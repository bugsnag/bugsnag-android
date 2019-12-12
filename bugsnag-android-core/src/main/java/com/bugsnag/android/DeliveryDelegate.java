package com.bugsnag.android;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.RejectedExecutionException;

class DeliveryDelegate extends BaseObservable {

    final Logger logger;
    private final EventStore eventStore;
    private final ImmutableConfig immutableConfig;
    final BreadcrumbState breadcrumbState;

    DeliveryDelegate(Logger logger, EventStore eventStore,
                     ImmutableConfig immutableConfig, BreadcrumbState breadcrumbState) {
        this.logger = logger;
        this.eventStore = eventStore;
        this.immutableConfig = immutableConfig;
        this.breadcrumbState = breadcrumbState;
    }

    void deliver(@NonNull Event event) {
        // Build the report
        Report report = new Report(immutableConfig.getApiKey(), event);
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
            deliverReportAsync(event, report);
        }
    }

    private void deliverReportAsync(@NonNull Event event, Report report) {
        final Report finalReport = report;
        final Event finalEvent = event;

        // Attempt to send the report in the background
        try {
            Async.run(new Runnable() {
                @Override
                public void run() {
                    deliverReportInternal(finalReport, finalEvent);
                }
            });
        } catch (RejectedExecutionException exception) {
            cacheEvent(event, false);
            logger.w("Exceeded max queue count, saving to disk to send later");
        }
    }

    @VisibleForTesting
    DeliveryStatus deliverReportInternal(@NonNull Report report, @NonNull Event event) {
        DeliveryParams deliveryParams = immutableConfig.getErrorApiDeliveryParams();
        Delivery delivery = immutableConfig.getDelivery();
        DeliveryStatus deliveryStatus = delivery.deliver(report, deliveryParams);

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
            String name = errors.get(0).getErrorClass();
            String msg = errors.get(0).getErrorMessage();
            Map<String, Object> message = Collections.<String, Object>singletonMap("message", msg);
            breadcrumbState.add(new Breadcrumb(name, BreadcrumbType.ERROR, message, new Date()));
        }
    }
}
