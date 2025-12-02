package com.bugsnag.android;

import static com.bugsnag.android.SeverityReason.REASON_PROMISE_REJECTION;

import com.bugsnag.android.internal.ImmutableConfig;
import com.bugsnag.android.internal.InternalMetrics;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * An Event object represents a Throwable captured by Bugsnag and is available as a parameter on
 * an {@link OnErrorCallback}, where individual properties can be mutated before an error report is
 * sent to Bugsnag's API.
 */
@SuppressWarnings("ConstantConditions")
public class Event implements JsonStream.Streamable, MetadataAware, UserAware, FeatureFlagAware {

    private final EventInternal impl;
    private final Logger logger;

    Event(@Nullable Throwable originalError,
          @NonNull ImmutableConfig config,
          @NonNull SeverityReason severityReason,
          @NonNull Logger logger) {
        this(originalError, config, severityReason, new Metadata(), new FeatureFlags(), logger);
    }

    Event(@Nullable Throwable originalError,
          @NonNull ImmutableConfig config,
          @NonNull SeverityReason severityReason,
          @NonNull Metadata metadata,
          @NonNull FeatureFlags featureFlags,
          @NonNull Logger logger) {
        this(new EventInternal(originalError, config, severityReason, metadata, featureFlags),
                logger);
    }

    Event(@NonNull EventInternal impl, @NonNull Logger logger) {
        this.impl = impl;
        this.logger = logger;
    }

    private void logNull(String property) {
        logger.e("Invalid null value supplied to event." + property + ", ignoring");
    }

    /**
     * The {@link Throwable} object that caused the event in your application.
     * <p>
     * Manipulating this field does not affect the error information reported to the
     * Bugsnag dashboard. Use {@link Event#getErrors()} to access and amend the representation of
     * the error that will be sent.
     */
    @Nullable
    public Throwable getOriginalError() {
        return impl.getOriginalError();
    }

    /**
     * Information extracted from the {@link Throwable} that caused the event can be found in this
     * field. The list contains at least one {@link Error} that represents the thrown object
     * with subsequent elements in the list populated from {@link Throwable#getCause()}.
     * <p>
     * A reference to the actual {@link Throwable} object that caused the event is available
     * through {@link Event#getOriginalError()} ()}.
     */
    @NonNull
    public List<Error> getErrors() {
        return impl.getErrors();
    }

    /**
     * Add a new error to this event and return its Error data. The new Error will appear at the
     * end of the {@link #getErrors() errors list}.
     */
    @NonNull
    public Error addError(@NonNull Throwable error) {
        return impl.addError(error);
    }

    /**
     * Add a new empty {@link ErrorType#ANDROID android} error to this event and return its Error
     * data. The new Error will appear at the end of the {@link #getErrors() errors list}.
     */
    @NonNull
    public Error addError(@NonNull String errorClass, @Nullable String errorMessage) {
        return impl.addError(errorClass, errorMessage, ErrorType.ANDROID);
    }

    /**
     * Add a new empty error to this event and return its Error data. The new Error will appear
     * at the end of the {@link #getErrors() errors list}.
     */
    @NonNull
    public Error addError(@NonNull String errorClass,
                          @Nullable String errorMessage,
                          @NonNull ErrorType errorType) {
        return impl.addError(errorClass, errorMessage, errorType);
    }

    /**
     * If thread state is being captured along with the event, this field will contain a
     * list of {@link Thread} objects.
     */
    @NonNull
    public List<Thread> getThreads() {
        return impl.getThreads();
    }

    /**
     * Create, add and return a new empty {@link Thread} object to this event with a given id
     * and name. This can be used to augment the event with thread data that would not be picked
     * up as part of a normal event being generated (for example: native threads managed
     * by cross-platform toolkits).
     *
     * @return a new Thread object of type {@link ErrorType#ANDROID} with no stacktrace
     */
    @NonNull
    public Thread addThread(@NonNull String id,
                            @NonNull String name) {
        return impl.addThread(
                id,
                name,
                ErrorType.ANDROID,
                false,
                Thread.State.RUNNABLE.getDescriptor()
        );
    }

    /**
     * Create, add and return a new empty {@link Thread} object to this event with a given id
     * and name. This can be used to augment the event with thread data that would not be picked
     * up as part of a normal event being generated (for example: native threads managed
     * by cross-platform toolkits).
     *
     * @return a new Thread object of type {@link ErrorType#ANDROID} with no stacktrace
     */
    @NonNull
    public Thread addThread(long id,
                            @NonNull String name) {
        return impl.addThread(
                Long.toString(id),
                name,
                ErrorType.ANDROID,
                false,
                Thread.State.RUNNABLE.getDescriptor()
        );
    }

    /**
     * Sets the thread that caused the event. This should be one of the threads in
     * {@link #getThreads()}. If the thread is not already in the list this method has no effect.
     *
     * @param thread the thread that caused the event
     * @see #setErrorReportingThread(long)
     */
    public void setErrorReportingThread(@NonNull Thread thread) {
        if (thread != null) {
            impl.setErrorReportingThread(thread);
        }
    }

    /**
     * Sets the thread that caused the event by its id. This should be one of the threads in
     * {@link #getThreads()}. If the id does not match any threads in the list this method has
     * no effect.
     *
     * @param threadId the id of the thread that caused the event
     * @see #setErrorReportingThread(Thread)
     */
    public void setErrorReportingThread(long threadId) {
        impl.setErrorReportingThread(threadId);
    }

    /**
     * A list of breadcrumbs leading up to the event. These values can be accessed and amended
     * if necessary. See {@link Breadcrumb} for details of the data available.
     */
    @NonNull
    public List<Breadcrumb> getBreadcrumbs() {
        return impl.getBreadcrumbs();
    }

    /**
     * Add a new breadcrumb to this event and return its Breadcrumb object. The new breadcrumb
     * will be added to the end of the {@link #getBreadcrumbs() breadcrumbs list} by this method.
     */
    @NonNull
    public Breadcrumb leaveBreadcrumb(@NonNull String message,
                                      @NonNull BreadcrumbType type,
                                      @Nullable Map<String, Object> metadata) {
        return impl.leaveBreadcrumb(message, type, metadata);
    }

    /**
     * Add a new breadcrumb to this event and return its Breadcrumb object. The new breadcrumb
     * will be added to the end of the {@link #getBreadcrumbs() breadcrumbs list} by this# method.
     */
    @NonNull
    public Breadcrumb leaveBreadcrumb(@NonNull String message) {
        return impl.leaveBreadcrumb(message, BreadcrumbType.MANUAL, null);
    }

    /**
     * A list of feature flags active at the time of the event.
     * See {@link FeatureFlag} for details of the data available.
     */
    @NonNull
    public List<FeatureFlag> getFeatureFlags() {
        return impl.getFeatureFlags().toList();
    }

    /**
     * Information set by the notifier about your app can be found in this field. These values
     * can be accessed and amended if necessary.
     */
    @NonNull
    public AppWithState getApp() {
        return impl.getApp();
    }

    /**
     * Information set by the notifier about your device can be found in this field. These values
     * can be accessed and amended if necessary.
     */
    @NonNull
    public DeviceWithState getDevice() {
        return impl.getDevice();
    }

    /**
     * The API key used for events sent to Bugsnag. Even though the API key is set when Bugsnag
     * is initialized, you may choose to send certain events to a different Bugsnag project.
     */
    public void setApiKey(@NonNull String apiKey) {
        if (apiKey != null) {
            impl.setApiKey(apiKey);
        } else {
            logNull("apiKey");
        }
    }

    /**
     * The API key used for events sent to Bugsnag. Even though the API key is set when Bugsnag
     * is initialized, you may choose to send certain events to a different Bugsnag project.
     */
    @NonNull
    public String getApiKey() {
        return impl.getApiKey();
    }

    /**
     * The severity of the event. By default, unhandled exceptions will be {@link Severity#ERROR}
     * and handled exceptions sent with {@link Bugsnag#notify} {@link Severity#WARNING}.
     */
    public void setSeverity(@NonNull Severity severity) {
        if (severity != null) {
            impl.setSeverity(severity);
        } else {
            logNull("severity");
        }
    }

    /**
     * The severity of the event. By default, unhandled exceptions will be {@link Severity#ERROR}
     * and handled exceptions sent with {@link Bugsnag#notify} {@link Severity#WARNING}.
     */
    @NonNull
    public Severity getSeverity() {
        return impl.getSeverity();
    }

    /**
     * Set the grouping hash of the event to override the default grouping on the dashboard.
     * All events with the same grouping hash will be grouped together into one error. This is an
     * advanced usage of the library and mis-using it will cause your events not to group properly
     * in your dashboard.
     * <p>
     * As the name implies, this option accepts a hash of sorts.
     */
    public void setGroupingHash(@Nullable String groupingHash) {
        impl.setGroupingHash(groupingHash);
    }

    /**
     * Set the grouping hash of the event to override the default grouping on the dashboard.
     * All events with the same grouping hash will be grouped together into one error. This is an
     * advanced usage of the library and mis-using it will cause your events not to group properly
     * in your dashboard.
     * <p>
     * As the name implies, this option accepts a hash of sorts.
     */
    @Nullable
    public String getGroupingHash() {
        return impl.getGroupingHash();
    }

    /**
     * Sets the context of the error. The context is a summary what what was occurring in the
     * application at the time of the crash, if available, such as the visible activity.
     */
    public void setContext(@Nullable String context) {
        impl.setContext(context);
    }

    /**
     * Returns the context of the error. The context is a summary what what was occurring in the
     * application at the time of the crash, if available, such as the visible activity.
     */
    @Nullable
    public String getContext() {
        return impl.getContext();
    }

    /**
     * Set the Grouping Discriminator for this {@code Event}
     * and return its previous value (if any was set).
     *
     * @param groupingDiscriminator the new grouping discriminator or null to clear it
     * @return the previously set grouping discriminator if one was set
     */
    @Nullable
    public String setGroupingDiscriminator(@Nullable String groupingDiscriminator) {
        String previousGroupingDiscriminator = impl.getGroupingDiscriminator();
        impl.setGroupingDiscriminator(groupingDiscriminator);
        return previousGroupingDiscriminator;
    }

    @Nullable
    public String getGroupingDiscriminator() {
        return impl.getGroupingDiscriminator();
    }

    /**
     * Sets the user associated with the event.
     */
    @Override
    public void setUser(@Nullable String id, @Nullable String email, @Nullable String name) {
        impl.setUser(id, email, name);
    }

    /**
     * Returns the currently set User information.
     */
    @Override
    @NonNull
    public User getUser() {
        return impl.getUser();
    }

    /**
     * Adds a map of multiple metadata key-value pairs to the specified section.
     */
    @Override
    public void addMetadata(@NonNull String section, @NonNull Map<String, ?> value) {
        if (section != null && value != null) {
            impl.addMetadata(section, value);
        } else {
            logNull("addMetadata");
        }
    }

    /**
     * Adds the specified key and value in the specified section. The value can be of
     * any primitive type or a collection such as a map, set or array.
     */
    @Override
    public void addMetadata(@NonNull String section, @NonNull String key, @Nullable Object value) {
        if (section != null && key != null) {
            impl.addMetadata(section, key, value);
        } else {
            logNull("addMetadata");
        }
    }

    /**
     * Removes all the data from the specified section.
     */
    @Override
    public void clearMetadata(@NonNull String section) {
        if (section != null) {
            impl.clearMetadata(section);
        } else {
            logNull("clearMetadata");
        }
    }

    /**
     * Removes data with the specified key from the specified section.
     */
    @Override
    public void clearMetadata(@NonNull String section, @NonNull String key) {
        if (section != null && key != null) {
            impl.clearMetadata(section, key);
        } else {
            logNull("clearMetadata");
        }
    }

    /**
     * Returns a map of data in the specified section.
     */
    @Override
    @Nullable
    public Map<String, Object> getMetadata(@NonNull String section) {
        if (section != null) {
            return impl.getMetadata(section);
        } else {
            logNull("getMetadata");
            return null;
        }
    }

    /**
     * Returns the value of the specified key in the specified section.
     */
    @Override
    @Nullable
    public Object getMetadata(@NonNull String section, @NonNull String key) {
        if (section != null && key != null) {
            return impl.getMetadata(section, key);
        } else {
            logNull("getMetadata");
            return null;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addFeatureFlag(@NonNull String name) {
        if (name != null) {
            impl.addFeatureFlag(name);
        } else {
            logNull("addFeatureFlag");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addFeatureFlag(@NonNull String name, @Nullable String variant) {
        if (name != null) {
            impl.addFeatureFlag(name, variant);
        } else {
            logNull("addFeatureFlag");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addFeatureFlags(@NonNull Iterable<FeatureFlag> featureFlags) {
        if (featureFlags != null) {
            impl.addFeatureFlags(featureFlags);
        } else {
            logNull("addFeatureFlags");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void clearFeatureFlag(@NonNull String name) {
        if (name != null) {
            impl.clearFeatureFlag(name);
        } else {
            logNull("clearFeatureFlag");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void clearFeatureFlags() {
        impl.clearFeatureFlags();
    }

    @Override
    public void toStream(@NonNull JsonStream stream) throws IOException {
        impl.toStream(stream);
    }

    /**
     * Whether the event was a crash (i.e. unhandled) or handled error in which the system
     * continued running.
     *
     * Unhandled errors count towards your stability score. If you don't want certain errors
     * to count towards your stability score, you can alter this property through an
     * {@link OnErrorCallback}.
     */
    public boolean isUnhandled() {
        return impl.getUnhandled();
    }

    /**
     * Whether the event was a crash (i.e. unhandled) or handled error in which the system
     * continued running.
     *
     * Unhandled errors count towards your stability score. If you don't want certain errors
     * to count towards your stability score, you can alter this property through an
     * {@link OnErrorCallback}.
     */
    public void setUnhandled(boolean unhandled) {
        impl.setUnhandled(unhandled);
    }

    /**
     * Associate this event with a specific trace. This is usually done automatically when
     * using bugsnag-android-performance, but can also be set manually if required.
     *
     * @param traceId the ID of the trace the event occurred within
     * @param spanId  the ID of the span that the event occurred within
     */
    public void setTraceCorrelation(@NonNull UUID traceId, long spanId) {
        if (traceId != null) {
            impl.setTraceCorrelation(new TraceCorrelation(traceId, spanId));
        } else {
            logNull("traceId");
        }
    }

    /**
     * Returns the delivery strategy for this event, which determines how the event
     * should be delivered to the Bugsnag API.
     *
     * @return the delivery strategy, or null if no specific strategy is set
     */
    @NonNull
    public DeliveryStrategy getDeliveryStrategy() {
        if (impl.getDeliveryStrategy() != null) {
            return impl.getDeliveryStrategy();
        }

        if (impl.getOriginalUnhandled()) {
            String severityReasonType = impl.getSeverityReasonType();
            boolean promiseRejection = REASON_PROMISE_REJECTION.equals(severityReasonType);
            boolean anr = impl.isAnr(this);
            if (anr || promiseRejection) {
                return DeliveryStrategy.STORE_AND_FLUSH;
            } else if (impl.isAttemptDeliveryOnCrash()) {
                return DeliveryStrategy.STORE_AND_SEND;
            } else {
                return DeliveryStrategy.STORE_ONLY;
            }
        } else {
            return DeliveryStrategy.SEND_IMMEDIATELY;
        }
    }

    /**
     * Sets the delivery strategy for this event, which determines how the event
     * should be delivered to the Bugsnag API. This allows customization of delivery
     * behavior on a per-event basis.
     *
     * @param deliveryStrategy the delivery strategy to use for this event
     */
    public void setDeliveryStrategy(@NonNull DeliveryStrategy deliveryStrategy) {
        if (deliveryStrategy != null) {
            impl.setDeliveryStrategy(deliveryStrategy);
        } else {
            logNull("deliveryStrategy");
        }
    }

    /**
     * Returns the HTTP request associated with this event, if any. This represents
     * the HTTP request that was being processed when the event occurred.
     *
     * The request object contains information such as the HTTP method, URL, headers,
     * and query parameters. This can be useful for understanding the context of errors
     * that occur during HTTP request handling.
     *
     * @return the HTTP request, or null if no request is associated with this event
     * @see Request
     * @see #setRequest(Request)
     */
    @Nullable
    public Request getRequest() {
        return impl.getRequest();
    }

    /**
     * Associates an HTTP request with this event. This should represent the HTTP request
     * that was being processed when the event occurred.
     *
     * Setting request information can help with debugging by providing context about
     * the HTTP request that led to the error. Set this to null to clear any previously
     * associated request.
     *
     * @param request the HTTP request to associate with this event, or null to clear it
     * @see Request
     * @see #getRequest()
     */
    public void setRequest(@Nullable Request request) {
        impl.setRequest(request);
    }

    /**
     * Returns the HTTP response associated with this event, if any. This represents
     * the HTTP response that was being generated when the event occurred.
     *
     * The response object contains information such as the HTTP status code, headers,
     * and body length. This can be useful for understanding the context of errors
     * that occur during HTTP response generation.
     *
     * @return the HTTP response, or null if no response is associated with this event
     * @see Response
     * @see #setResponse(Response)
     */
    @Nullable
    public Response getResponse() {
        return impl.getResponse();
    }

    /**
     * Associates an HTTP response with this event. This should represent the HTTP response
     * that was being generated when the event occurred.
     *
     * Setting response information can help with debugging by providing context about
     * the HTTP response generation that led to the error. Set this to null to clear any
     * previously associated response.
     *
     * @param response the HTTP response to associate with this event, or null to clear it
     * @see Response
     * @see #setResponse(Response)
     */
    public void setResponse(@Nullable Response response) {
        impl.setResponse(response);
    }

    protected boolean shouldDiscardClass() {
        return impl.shouldDiscardClass();
    }

    protected void updateSeverityInternal(@NonNull Severity severity) {
        impl.updateSeverityInternal(severity);
    }

    protected void updateSeverityReason(@NonNull @SeverityReason.SeverityReasonType String reason) {
        impl.updateSeverityReason(reason);
    }

    void setApp(@NonNull AppWithState app) {
        impl.setApp(app);
    }

    void setDevice(@NonNull DeviceWithState device) {
        impl.setDevice(device);
    }

    void setBreadcrumbs(@NonNull List<Breadcrumb> breadcrumbs) {
        impl.setBreadcrumbs(breadcrumbs);
    }

    @Nullable
    Session getSession() {
        return impl.session;
    }

    void setSession(@Nullable Session session) {
        impl.session = session;
    }

    EventInternal getImpl() {
        return impl;
    }

    void setRedactedKeys(Collection<Pattern> redactedKeys) {
        impl.setRedactedKeys(redactedKeys);
    }

    void setInternalMetrics(InternalMetrics metrics) {
        impl.setInternalMetrics(metrics);
    }
}
