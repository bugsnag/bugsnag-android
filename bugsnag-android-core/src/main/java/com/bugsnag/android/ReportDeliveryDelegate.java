package com.bugsnag.android;

import androidx.annotation.NonNull;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.concurrent.RejectedExecutionException;

class ReportDeliveryDelegate extends BaseObservable {

    final Logger logger;
    final EventStore eventStore;
    final ImmutableConfig immutableConfig;
    final BreadcrumbState breadcrumbState;

    ReportDeliveryDelegate(Logger logger, EventStore eventStore,
                           ImmutableConfig immutableConfig, BreadcrumbState breadcrumbState) {
        this.logger = logger;
        this.eventStore = eventStore;
        this.immutableConfig = immutableConfig;
        this.breadcrumbState = breadcrumbState;
    }

    void deliverEvent(@NonNull Event event) {
        // increment handled counts after error not rejected by callbacks
        Session session = event.getSession();

        if (session != null) {
            if (event.getUnhandled()) {
                event.setSession(session.incrementUnhandledAndCopy());
            } else {
                event.setSession(session.incrementHandledAndCopy());
            }
        }

        // Build the report
        Report report = new Report(event.getApiKey(), null, event);

        if (event.getSession() != null) {
            if (event.getUnhandled()) {
                notifyObservers(StateEvent.NotifyUnhandled.INSTANCE);
            } else {
                List<Error> errors = event.getErrors();

                String name = "";

                if (errors.size() > 0) {
                    name = errors.get(0).getErrorClass();
                }
                notifyObservers(StateEvent.NotifyHandled.INSTANCE);
            }
        }

        if (event.getUnhandled()) {
            eventStore.write(event);
            eventStore.flushAsync();
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
                    deliver(finalReport, finalEvent);
                }
            });
        } catch (RejectedExecutionException exception) {
            eventStore.write(event);
            logger.w("Exceeded max queue count, saving to disk to send later");
        }
    }

    DeliveryStatus deliver(@NonNull Report report, @NonNull Event event) {
        DeliveryParams deliveryParams = immutableConfig.errorApiDeliveryParams();
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
                eventStore.write(event);
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

    private void leaveErrorBreadcrumb(@NonNull Event event) {
        // Add a breadcrumb for this event occurring
        List<Error> errors = event.getErrors();

        String name = "";
        String msg = "";

        if (errors.size() > 0) {
            name = errors.get(0).getErrorClass();
            msg = errors.get(0).getErrorMessage();
        }

        Map<String, Object> message = Collections.<String, Object>singletonMap("message", msg);
        breadcrumbState.add(new Breadcrumb(name, BreadcrumbType.ERROR, message, new Date()));
    }
}
