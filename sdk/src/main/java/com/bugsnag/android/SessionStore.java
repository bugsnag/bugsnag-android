package com.bugsnag.android;

import android.content.Context;
import android.support.annotation.NonNull;

import java.io.File;
import java.util.Comparator;
import java.util.Locale;

/**
 * Store and flush Sessions which couldn't be sent immediately due to
 * lack of network connectivity.
 */
class SessionStore extends FileStore<SessionTrackingPayload> {

    static final Comparator<File> SESSION_COMPARATOR = new Comparator<File>() {
        @Override
        public int compare(File lhs, File rhs) {
            if (lhs == null && rhs == null) {
                return 0;
            }
            if (lhs == null) {
                return 1;
            }
            if (rhs == null) {
                return -1;
            }
            String lhsName = lhs.getName();
            String rhsName = rhs.getName();
            return lhsName.compareTo(rhsName);
        }
    };

    SessionStore(@NonNull Configuration config, @NonNull Context appContext) {
        super(config, appContext, "/bugsnag-sessions/", 128, SESSION_COMPARATOR);
    }

    @NonNull
    @Override
    String getFilename(SessionTrackingPayload session) {
        return String.format(Locale.US, "%s%d.json", storeDirectory, System.currentTimeMillis());
    }

}
