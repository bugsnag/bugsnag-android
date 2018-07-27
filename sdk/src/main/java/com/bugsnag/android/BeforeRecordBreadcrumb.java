package com.bugsnag.android;

import android.support.annotation.NonNull;

import com.facebook.infer.annotation.ThreadSafe;

/**
 * Add a "before breadcrumb" callback, to execute code before every
 * breadcrumb captured by Bugsnag.
 * <p>
 * You can use this to modify breadcrumbs before they are stored by Bugsnag.
 * You can also return <code>false</code> from any callback to ignore a breadcrumb.
 * <p>
 * For example:
 * <p>
 * Bugsnag.beforeRecordBreadcrumb(new BeforeRecordBreadcrumb() {
 * public boolean shouldRecord(Breadcrumb breadcrumb) {
 * return false; // ignore the breadcrumb
 * }
 * })
 */
@ThreadSafe
public interface BeforeRecordBreadcrumb {

    /**
     * Runs the "before breadcrumb" callback. If the callback returns
     * <code>false</code> any further BeforeRecordBreadcrumb callbacks will not be called
     * and the breadcrumb will not be captured by Bugsnag.
     *
     * @param breadcrumb the breadcrumb to be captured by Bugsnag
     * @see Breadcrumb
     */
    boolean shouldRecord(@NonNull Breadcrumb breadcrumb);

}
