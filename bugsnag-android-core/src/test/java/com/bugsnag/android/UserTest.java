package com.bugsnag.android;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

public class UserTest {

    private User user;

    @Before
    public void setUp() {
        user = new User("123", "bob@example.com", "bob smith");
    }

    @Test
    public void testUserDefaults() {
        assertEquals("123", user.getId());
        assertEquals("bob smith", user.getName());
        assertEquals("bob@example.com", user.getEmail());
    }
}
