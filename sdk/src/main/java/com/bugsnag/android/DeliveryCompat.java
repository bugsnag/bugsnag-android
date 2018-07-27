package com.bugsnag.android;

import com.facebook.infer.annotation.ThreadSafe;

/**
 * A compatibility implementation of {@link Delivery} which wraps {@link ErrorReportApiClient} and
 * {@link SessionTrackingApiClient}. This class allows for backwards compatibility for users still
 * utilising the old API, and should be removed in the next major version.
 */
@ThreadSafe
class DeliveryCompat implements Delivery {

    // ignore deprecation of legacy clients
    @SuppressWarnings("deprecation")
    volatile ErrorReportApiClient errorReportApiClient;
    @SuppressWarnings("deprecation")
    volatile SessionTrackingApiClient sessionTrackingApiClient;

    @Override
    public void deliver(SessionTrackingPayload payload,
                        Configuration config) throws DeliveryFailureException {
        if (sessionTrackingApiClient != null) {
            try {
                sessionTrackingApiClient.postSessionTrackingPayload(config.getSessionEndpoint(),
                    payload, config.getSessionApiHeaders());
            } catch (Throwable throwable) {
                handleException(throwable);
            }
        }
    }

    @Override
    public void deliver(Report report, Configuration config) throws DeliveryFailureException {
        if (errorReportApiClient != null) {
            try {
                errorReportApiClient.postReport(config.getEndpoint(),
                    report, config.getErrorApiHeaders());
            } catch (Throwable throwable) {
                handleException(throwable);
            }
        }
    }

    @SuppressWarnings("deprecation") // ignore networkexception deprecation
    void handleException(Throwable throwable) throws DeliveryFailureException {
        if (throwable instanceof NetworkException) {
            throw new DeliveryFailureException(throwable.getMessage(), throwable);
        } else {
            Logger.warn("Ignoring Exception, this API is deprecated", throwable);
        }
    }

}
