package com.bugsnag.android;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

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

}
