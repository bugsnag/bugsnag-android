package com.bugsnag.android;

import android.support.test.filters.LargeTest;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;

import com.bugsnag.android.example.ExampleActivity;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.isRoot;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class AppSmokeTest {

    @Rule
    public ActivityTestRule<ExampleActivity> activityTestRule = new ActivityTestRule<>(ExampleActivity.class);

    @Test
    public void checkAppLaunches() throws Exception {
        onView(isRoot()).check(matches(isDisplayed()));
    }

}
