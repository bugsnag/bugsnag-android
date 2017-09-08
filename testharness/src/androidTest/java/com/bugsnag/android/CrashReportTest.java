package com.bugsnag.android;

import android.support.test.annotation.UiThreadTest;
import android.support.test.filters.LargeTest;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;

import com.bugsnag.android.testharness.TestHarnessActivity;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static junit.framework.Assert.assertNotNull;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class CrashReportTest {

    @Rule
    public ActivityTestRule<TestHarnessActivity> activityTestRule = new ActivityTestRule<>(TestHarnessActivity.class);

    @Test
    @UiThreadTest
    public void checkAppLaunches() throws Exception {
        final TestHarnessActivity activity = activityTestRule.getActivity();
        assertNotNull(activity);

        try {
            activity.crashApp();
        } catch (RuntimeException ignored) {
        }
    }

}
