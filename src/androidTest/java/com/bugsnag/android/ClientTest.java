package com.bugsnag.android;

public class ClientTest extends BugsnagTestCase {
    public void testNullContext() {
        try {
            Client client = new Client(null, "api-key");
            fail("Should throw for null Contexts");
        } catch(NullPointerException e) { }
    }

    public void testNullApiKey() {
        try {
            Client client = new Client(getContext(), null, true);
            fail("Should throw for null Contexts");
        } catch(NullPointerException e) { }
    }

    public void testNotify() {
        // Notify should not crash
        Client client = new Client(getContext(), "api-key");
        client.notify(new RuntimeException("Testing"));
    }

    public void testConfig() {
        Configuration config = new Configuration("api-key");
        config.setEndpoint("new-endpoint");

        Client client = new Client(getContext(), config);

        // Notify should not crash
        client.notify(new RuntimeException("Testing"));
    }

}
