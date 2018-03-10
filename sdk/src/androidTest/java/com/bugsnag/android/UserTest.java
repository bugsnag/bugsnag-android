package com.bugsnag.android;

import android.content.SharedPreferences;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.UUID;

import static com.bugsnag.android.BugsnagTestUtils.getSharedPrefs;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.not;
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

    @Test
    public void testRepo() {
        SharedPreferences sharedPref = getSharedPrefs(InstrumentationRegistry.getContext());

        User pre = User.builder()
            .name(UUID.randomUUID().toString())
            .id(UUID.randomUUID().toString())
            .email(UUID.randomUUID().toString())
            .build();

        User.Repo repo = new User.Repo(sharedPref);
        repo.set(pre);

        User post = repo.get();
        assertThat(post, is(not(nullValue())));

        assertThat(post.getId(), is(pre.getId()));
        assertThat(post.getEmail(), is(pre.getEmail()));
        assertThat(post.getName(), is(pre.getName()));

        repo.set(null);
        assertThat(repo.get(), is(nullValue()));
    }
}
