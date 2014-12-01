package com.bugsnag.android;

public class ClientTest extends BugsnagTestCase {
    public void testConstructor() {
        // Should not allow creating a client with a null Context
        try {
            Client client = new Client(null, "api-key");
            fail("Should throw for null Contexts");
        } catch(RuntimeException e) { }

        // Should not allow creating a client with a null apiKey
        try {
            Client client = new Client(getContext(), null);
            fail("Should throw for null api key");
        } catch(RuntimeException e) { }

        // Notify should not crash
        Client client = new Client(getContext(), "api-key");
        client.notify(new RuntimeException("Testing"));
    }
}
