package com.bugsnag.android;

/**
 * A callback to be run before every report to Bugsnag.
 * <p>
 * <p>You can use this to add or modify information attached to an error
 * before it is sent to your dashboard. You can also return
 * <code>false</code> from any callback to halt execution.
 */
public interface BeforeNotify {
    /**
     * Runs the "before notify" callback. If the callback returns
     * <code>false</code> any further BeforeNotify callbacks will not be called
     * and the error will not be sent to Bugsnag.
     *
     * @param error the error to be sent to Bugsnag
     * @see Error
     */
    boolean run(Error error);
}
