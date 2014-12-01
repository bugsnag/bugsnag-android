package com.bugsnag.android;

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
