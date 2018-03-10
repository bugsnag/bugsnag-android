package com.bugsnag.android;

import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.UUID;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.Is.is;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class UserTest {
    @Test
    public void testBuilder_defaults() {
        User user = User.builder().build();

        assertThat(user.getId(), is(nullValue()));
        assertThat(user.getEmail(), is(nullValue()));
        assertThat(user.getName(), is(nullValue()));
    }

    @Test
    public void testBuilder_values() {
        final String id = UUID.randomUUID().toString();
        final String email = UUID.randomUUID().toString();
        final String name = UUID.randomUUID().toString();

        User user = User.builder().name(name).id(id).email(email).build();

        assertThat(user.getId(), is(id));
        assertThat(user.getEmail(), is(email));
        assertThat(user.getName(), is(name));
    }

    @Test
    public void testBuilder_fromPrevious() {
        final String id = UUID.randomUUID().toString();
        final String email = UUID.randomUUID().toString();
        final String name = UUID.randomUUID().toString();

        User previous = User.builder().name(name).id(id).email(email).build();
        User successor = User.builder(previous).build();

        assertThat(successor.getId(), is(id));
        assertThat(successor.getEmail(), is(email));
        assertThat(successor.getName(), is(name));
    }
}
