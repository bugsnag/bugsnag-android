package com.bugsnag.android;

import android.support.test.runner.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;

@RunWith(AndroidJUnit4.class)
public class BeforeNotifyTest {

    BeforeNotify beforeNotify = new BeforeNotify() {
        @Override
        public boolean run(Error error) {
            return true;
        }
    };

    BeforeNotify beforeNotifySkip = new BeforeNotify() {
        @Override
        public boolean run(Error error) {
            return false;
        }
    };

    @Test
    public void testRunModifiesError() {
        BeforeNotify beforeNotify = new BeforeNotify() {
            @Override
            public boolean run(Error error) {
                error.setContext("new-context");
                return false;
            }
        };

        Configuration config = new Configuration("api-key");
        Error error = new Error(config, new RuntimeException("Test"));
        beforeNotify.run(error);

        assertEquals("new-context", error.getContext());
    }
}
