package com.bugsnag.android;

import android.content.Context;
import android.content.SharedPreferences;

public class ClientTest extends BugsnagTestCase {
    public void testNullContext() {
        try {
            Client client = new Client(null, "api-key");
            fail("Should throw for null Contexts");
        } catch(NullPointerException e) { }
    }

    public void testNullApiKey() {
        try {
            Client client = new Client(getContext(), null);
            fail("Should throw for null API Key");
        } catch(NullPointerException e) { }
    }

    public void testNotify() {
        // Notify should not crash
        Client client = new Client(getContext(), "api-key");
        client.notify(new RuntimeException("Testing"));
    }

    public void testRestoreUserFromPrefs() {
        // Ensure that there is no user set in prefs to start with
        Client client = new Client(getContext(), "api-key");
        client.clearUser();

        // Set a user in prefs
        SharedPreferences sharedPref = getContext().getSharedPreferences("com.bugsnag.android", Context.MODE_PRIVATE);
        sharedPref.edit()
            .putString("user.id", "123456")
            .putString("user.email", "mr.test@email.com")
            .putString("user.name", "Mr Test")
            .commit();

        client = new Client(getContext(), "api-key");
        final User user = new User();

        client.beforeNotify(new BeforeNotify() {
            @Override
            public boolean run(Error error) {
                // Pull out the user information
                user.setId(error.getUser().getId());
                user.setEmail(error.getUser().getEmail());
                user.setName(error.getUser().getName());
                return true;
            }
        });

        client.notify(new RuntimeException("Testing"));

        // Check the user details have been set
        assertEquals("123456", user.getId());
        assertEquals("mr.test@email.com", user.getEmail());
        assertEquals("Mr Test", user.getName());

        // Tidy up
        client.clearUser();
    }

    public void testStoreUserInPrefs() {
        // Ensure that there is no user set in prefs to start with
        Client client = new Client(getContext(), "api-key");
        client.clearUser();

        client = new Client(getContext(), "api-key");
        client.setUser("123456", "mr.test@email.com", "Mr Test");

        // Check that the user was store in prefs
        SharedPreferences sharedPref = getContext().getSharedPreferences("com.bugsnag.android", Context.MODE_PRIVATE);
        assertEquals("123456", sharedPref.getString("user.id", null));
        assertEquals("mr.test@email.com", sharedPref.getString("user.email", null));
        assertEquals("Mr Test", sharedPref.getString("user.name", null));

        // Tidy up
        client.clearUser();
    }

}
