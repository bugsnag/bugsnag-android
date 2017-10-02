package com.bugsnag.android;

import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class BeforeNotifyTest {

    private BeforeNotify beforeNotify = new BeforeNotify() {
        @Override
        public boolean run(Error error) {
            return true;
        }
    };

    private BeforeNotify beforeNotifySkip = new BeforeNotify() {
        @Override
        public boolean run(Error error) {
            return false;
        }
    };

    private Configuration config;

    @Before
    public void setUp() throws Exception {
        config = new Configuration("api-key");
    }

    @Test
    public void testRunModifiesError() {
        final String context = "new-context";

        BeforeNotify beforeNotify = new BeforeNotify() {
            @Override
            public boolean run(Error error) {
                error.setContext(context);
                return false;
            }
        };

        Error error = new Error.Builder(config, new RuntimeException("Test")).build();
        beforeNotify.run(error);

        assertEquals(context, error.getContext());
    }
}
