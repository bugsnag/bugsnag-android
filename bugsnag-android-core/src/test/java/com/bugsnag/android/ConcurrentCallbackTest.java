package com.bugsnag.android;

import androidx.annotation.NonNull;

import org.junit.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

import static com.bugsnag.android.BugsnagTestUtils.generateImmutableConfig;

/**
 * Ensures that if a callback is added or removed during iteration, a
 * {@link java.util.ConcurrentModificationException} is not thrown
 */
public class ConcurrentCallbackTest {

    private final HandledState handledState = HandledState.newInstance(HandledState.REASON_HANDLED_EXCEPTION);
    private final Event event = new Event(generateImmutableConfig(), handledState);
    private final SessionPayload sessionPayload = new SessionPayload(null, new ArrayList<File>(), new HashMap<String, Object>(), new HashMap<String, Object>());

    @Test
    public void testOnErrorConcurrentModification() {
        CallbackState config = new CallbackState();
        final Collection<OnError> tasks = config.getOnErrorTasks();
        config.addOnError(new OnError() {
            @Override
            public boolean run(@NonNull Event event) {
                tasks.add(new OnErrorSkeleton());
                // modify the Set, when iterating to the next callback this should not crash
                return true;
            }
        });
        config.addOnError(new OnErrorSkeleton());
        config.runOnErrorTasks(event, NoopLogger.INSTANCE);
    }

    @Test
    public void testOnBreadcrumbConcurrentModification() {
        CallbackState config = new CallbackState();
        final Collection<OnBreadcrumb> tasks = config.getOnBreadcrumbTasks();
        config.addOnBreadcrumb(new OnBreadcrumb() {
            @Override
            public boolean run(@NonNull Breadcrumb breadcrumb) {
                tasks.add(new OnBreadcrumbSkeleton());
                // modify the Set, when iterating to the next callback this should not crash
                return true;
            }
        });
        config.addOnBreadcrumb(new OnBreadcrumbSkeleton());
        config.runOnBreadcrumbTasks(new Breadcrumb("Foo"), NoopLogger.INSTANCE);
    }

    @Test
    public void testOnSessionConcurrentModification() {
        CallbackState config = new CallbackState();
        final Collection<OnSession> tasks = config.getOnSessionTasks();
        config.addOnSession(new OnSession() {
            @Override
            public boolean run(@NonNull SessionPayload event) {
                tasks.add(new OnSessionSkeleton());
                // modify the Set, when iterating to the next callback this should not crash
                return true;
            }
        });
        config.addOnSession(new OnSessionSkeleton());
        config.runOnSessionTasks(sessionPayload, NoopLogger.INSTANCE);
    }

    static class OnErrorSkeleton implements OnError {
        @Override
        public boolean run(@NonNull Event event) {
            return true;
        }
    }

    static class OnBreadcrumbSkeleton implements OnBreadcrumb {
        @Override
        public boolean run(@NonNull Breadcrumb breadcrumb) {
            return true;
        }
    }

    static class OnSessionSkeleton implements OnSession {
        @Override
        public boolean run(@NonNull SessionPayload sessionPayload) {
            return true;
        }
    }
}
