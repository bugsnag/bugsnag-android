package com.bugsnag.android;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class Client {

    protected final ImmutableConfig immutableConfig;
    protected final BreadcrumbState breadcrumbState;
    protected final Logger logger;

    // Package-private internal initializer core reference
    Client(@NonNull ImmutableConfig immutableConfig, @NonNull BreadcrumbState breadcrumbState, @NonNull Logger logger) {
        this.immutableConfig = immutableConfig;
        this.breadcrumbState = breadcrumbState;
        this.logger = logger;
    }

    /**
     * Standard implementation: Leave a breadcrumb tracking an immediate user step.
     */
    public void leaveBreadcrumb(@NonNull String name) {
        leaveBreadcrumb(name, BreadcrumbType.MANUAL, new HashMap<>());
    }

    /**
     * Standard implementation: Leave a breadcrumb with categorical meta routing context.
     */
    public void leaveBreadcrumb(@NonNull String name, @NonNull BreadcrumbType type, @NonNull Map<String, Object> metadata) {
        leaveBreadcrumb(name, type, metadata, null);
    }

    /**
     * New Extended API Feature: Leave a breadcrumb with a synchronized historical or external timestamp.
     *
     * @param name            the descriptive label for the user event
     * @param type            the categorized subsystem variant
     * @param metadata        additional diagnostic context parameters
     * @param customTimestamp an optional backdated instance timeline configuration
     */
    public void leaveBreadcrumb(
            @NonNull String name,
            @NonNull BreadcrumbType type,
            @NonNull Map<String, Object> metadata,
            @Nullable Date customTimestamp) {

        // Check internal discard exclusions before appending allocation footprints
        if (immutableConfig.shouldDiscardBreadcrumb(name)) {
            logger.info("Discarding breadcrumb according to configured filters: " + name);
            return;
        }

        // Initialize state element with our customized structural overload signatures
        final Breadcrumb breadcrumb = new Breadcrumb(name, type, metadata, customTimestamp);

        // Persist into current thread storage pool queue structures natively
        breadcrumbState.add(breadcrumb);
        logger.info("Successfully registered breadcrumb: " + name + " [Timestamp: " + breadcrumb.getTimestamp() + "]");
    }
}
