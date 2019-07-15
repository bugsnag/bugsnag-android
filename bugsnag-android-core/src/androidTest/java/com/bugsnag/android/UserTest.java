package com.bugsnag.android;

import static com.bugsnag.android.BugsnagTestUtils.streamableToJson;
import static org.junit.Assert.assertEquals;

import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class UserTest {

    private User user;

    @Before
    public void setUp() throws Exception {
        user = new User("123", "bob@example.com", "bob smith");
    }

    @Test
    public void testUserDefaults() {
        assertEquals("123", user.getId());
        assertEquals("bob smith", user.getName());
        assertEquals("bob@example.com", user.getEmail());
    }

    @Test
    public void testUserCopy() {
        user = new User(user);
        assertEquals("123", user.getId());
        assertEquals("bob smith", user.getName());
        assertEquals("bob@example.com", user.getEmail());
    }

    @Test
    public void testUserOverride() {
        user.setId("4fd");
        user.setName("jane");
        user.setEmail("jane@example.com");
        assertEquals("4fd", user.getId());
        assertEquals("jane", user.getName());
        assertEquals("jane@example.com", user.getEmail());
    }

    @Test
    public void testJsonSerialisation() throws JSONException, IOException {
        JSONObject userJson = streamableToJson(user);
        assertEquals(3, userJson.length());
        assertEquals("123", userJson.get("id"));
        assertEquals("bob smith", userJson.get("name"));
        assertEquals("bob@example.com", userJson.get("email"));
    }

}
