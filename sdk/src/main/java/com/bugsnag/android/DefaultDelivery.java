package com.bugsnag.android;

class DefaultDelivery implements Delivery {

    @Override
    public void deliver(SessionTrackingPayload payload,
                        Configuration config) throws DeliveryFailureException {

    }

    @Override
    public void deliver(Report report,
                        Configuration config) throws DeliveryFailureException {

    }

    private void deliver(JsonStream.Streamable streamable,
                         Configuration config) throws DeliveryFailureException {

    }
}
