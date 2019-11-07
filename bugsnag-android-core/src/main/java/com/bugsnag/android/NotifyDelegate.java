package com.bugsnag.android;

import static com.bugsnag.android.HandledState.REASON_HANDLED_EXCEPTION;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;

class NotifyDelegate {

    private final ImmutableConfig immutableConfig;
    private final MetadataState metadataState;
    private final UserState userState;
    private final ContextState contextState;
    private final BreadcrumbState breadcrumbState;
    private final CallbackState callbackState;
    private final AppData appData;
    private final DeviceData deviceData;
    private final SessionTracker sessionTracker;
    private final Logger logger;

    NotifyDelegate(ImmutableConfig immutableConfig, MetadataState metadataState,
                   UserState userState, ContextState contextState,
                   BreadcrumbState breadcrumbState, CallbackState callbackState,
                   AppData appData, DeviceData deviceData, SessionTracker sessionTracker,
                   Logger logger) {
        this.immutableConfig = immutableConfig;
        this.metadataState = metadataState;
        this.userState = userState;
        this.contextState = contextState;
        this.breadcrumbState = breadcrumbState;
        this.callbackState = callbackState;
        this.appData = appData;
        this.deviceData = deviceData;
        this.sessionTracker = sessionTracker;
        this.logger = logger;
    }

    Event notify(@NonNull Throwable exc, @Nullable OnError onError) {
        HandledState handledState = HandledState.newInstance(REASON_HANDLED_EXCEPTION);
        Event event = new Event(exc, immutableConfig, handledState, metadataState.getMetadata());
        return notifyInternal(event, onError);
    }

    Event notify(@NonNull String name,
                 @NonNull String message,
                 @NonNull StackTraceElement[] stacktrace,
                 @Nullable OnError onError) {
        HandledState handledState = HandledState.newInstance(REASON_HANDLED_EXCEPTION);
        Stacktrace trace = new Stacktrace(stacktrace, immutableConfig.getProjectPackages());
        Error err = new Error(name, message, trace.getTrace());
        Metadata metadata = metadataState.getMetadata();
        Event event = new Event(null, immutableConfig, handledState, metadata, stacktrace);
        event.setErrors(Collections.singletonList(err));
        return notifyInternal(event, onError);
    }

    Event notifyUnhandledException(@NonNull Throwable exc,
                                   @HandledState.SeverityReason String severityReason,
                                   @Nullable String attributeValue,
                                   java.lang.Thread thread, OnError onError) {
        HandledState handledState
                = HandledState.newInstance(severityReason, Severity.ERROR, attributeValue);
        ThreadState threadState = new ThreadState(immutableConfig, exc.getStackTrace(), thread);
        Event event = new Event(exc, immutableConfig, handledState,
                metadataState.getMetadata(), null, threadState);
        return notifyInternal(event, onError);
    }

    @Nullable
    Event notifyInternal(@NonNull Event event,
                         @Nullable OnError onError) {
        // Don't notify if this event class should be ignored
        if (event.shouldIgnoreClass()) {
            return null;
        }

        if (!immutableConfig.shouldNotifyForReleaseStage()) {
            return null;
        }

        // get session for event
        Session currentSession = sessionTracker.getCurrentSession();

        if (currentSession != null
                && (immutableConfig.getAutoTrackSessions() || !currentSession.isAutoCaptured())) {
            event.setSession(currentSession);
        }

        // Capture the state of the app and device and attach diagnostics to the event
        Map<String, Object> errorDeviceData = deviceData.getDeviceData();
        event.setDevice(errorDeviceData);
        event.addMetadata("device", null, deviceData.getDeviceMetadata());


        // add additional info that belongs in metadata
        // generate new object each time, as this can be mutated by end-users
        Map<String, Object> errorAppData = appData.getAppData();
        event.setApp(errorAppData);
        event.addMetadata("app", null, appData.getAppDataMetadata());

        // Attach breadcrumbState to the event
        event.setBreadcrumbs(new ArrayList<>(breadcrumbState.getStore()));

        // Attach user info to the event
        User user = userState.getUser();
        event.setUser(user.getId(), user.getEmail(), user.getName());

        // Attach default context from active activity
        if (Intrinsics.isEmpty(event.getContext())) {
            String context = contextState.getContext();
            event.setContext(context != null ? context : appData.getActiveScreenClass());
        }

        // Run on error tasks, don't notify if any return false
        if ((onError != null && !onError.run(event)
                || !callbackState.runOnErrorTasks(event, logger))) {
            logger.i("Skipping notification - onError task returned false");
            return null;
        }
        return event;
    }

}
