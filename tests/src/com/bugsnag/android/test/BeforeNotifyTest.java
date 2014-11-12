package com.bugsnag.android;

import com.bugsnag.android.BeforeNotify;
import com.bugsnag.android.Error;

public class BeforeNotifyTest extends BugsnagTestCase {
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

    @Override
    protected void setUp() {

    }

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

        assertEquals(error.getContext(), "new-context");
    }

    public void testRunAll() {
        // Any false beforeNotify should return false
        Configuration config = new Configuration("api-key");
        config.addBeforeNotify(beforeNotify);
        config.addBeforeNotify(beforeNotifySkip);

        Error error = new Error(config, new RuntimeException("Test"));
        assertFalse(BeforeNotify.runAll(config.beforeNotifyTasks, error));
    }
}
