package com.bugsnag.android;

import static com.bugsnag.android.DeliveryFailureException.Reason.CONNECTIVITY;
import static com.bugsnag.android.DeliveryFailureException.Reason.REQUEST_FAILURE;

class DeliveryCompat implements Delivery {

    final Delivery delivery;
    volatile ErrorReportApiClient errorReportApiClient;
    volatile SessionTrackingApiClient sessionTrackingApiClient;

    DeliveryCompat(Delivery delivery) {
        this.delivery = delivery;
    }

    @Override
    public void deliver(SessionTrackingPayload payload,
                        Configuration config) throws DeliveryFailureException {
        if (sessionTrackingApiClient != null) {

            try {
                sessionTrackingApiClient.postSessionTrackingPayload(config.getSessionEndpoint(),
                    payload, config.getSessionApiHeaders());
            } catch (NetworkException | BadResponseException exception) {
                throw convertException(exception);
            }
        } else {
            delivery.deliver(payload, config);
        }
    }

    @Override
    public void deliver(Report report, Configuration config) throws DeliveryFailureException {
        if (errorReportApiClient != null) {
            try {
                errorReportApiClient.postReport(config.getEndpoint(),
                    report, config.getErrorApiHeaders());
            } catch (NetworkException | BadResponseException exception) {
                throw convertException(exception);
            }
        } else {
            delivery.deliver(report, config);
        }
    }

    DeliveryFailureException convertException(Exception exception) {
        if (exception instanceof NetworkException) {
            return new DeliveryFailureException(CONNECTIVITY, exception.getMessage(), exception);
        } else if (exception instanceof BadResponseException) {
            return new DeliveryFailureException(REQUEST_FAILURE, exception.getMessage(), exception);
        } else {
            return null;
        }
    }
}
