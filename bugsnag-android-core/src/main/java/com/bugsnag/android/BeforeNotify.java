package com.bugsnag.android;

import androidx.annotation.NonNull;

/**
 * A callback to be run before reports are sent to Bugsnag.
 * <p>
 * <p>You can use this to add or modify information attached to an error
 * before it is sent to your dashboard. You can also return
 * <code>false</code> from any callback to halt execution.
 * <p>"Before notify" callbacks do not run when a fatal C/C++ crash occurs.
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
    boolean run(@NonNull Error error);
}
