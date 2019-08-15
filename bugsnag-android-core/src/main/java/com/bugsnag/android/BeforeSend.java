package com.bugsnag.android;

import androidx.annotation.NonNull;

/**
 * A callback to be run before every report sent to Bugsnag.
 * <p>
 * <p>You can use this to add or modify information attached to an error
 * before it is delivered to your dashboard. You can also return
 * <code>false</code> from any callback to halt execution.
 */
@ThreadSafe
public interface BeforeSend {
    /**
     * Runs the callback. If the callback returns
     * <code>false</code> any further BeforeSend callbacks will not be called
     * and the report will not be sent to Bugsnag.
     *
     * @param report the {@link Report} to be sent to Bugsnag
     */
    boolean run(@NonNull Report report);
}
