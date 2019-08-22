package com.bugsnag.android;

import androidx.test.filters.SmallTest;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

import static com.bugsnag.android.BugsnagTestUtils.streamableToJson;
import static org.junit.Assert.assertEquals;

@SmallTest
public class UserSerializationTest {

    private User user;

    @Before
    public void setUp() throws Exception {
        user = new User("123", "bob@example.com", "bob smith");
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
