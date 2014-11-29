package com.bugsnag.android;

import java.util.Collection;

public abstract class BeforeNotify {
    public abstract boolean run(Error error);

    static boolean runAll(Collection<BeforeNotify> beforeNotifyTasks, Error error) {
        for (BeforeNotify beforeNotify : beforeNotifyTasks) {
            try {
                if (!beforeNotify.run(error)) {
                    return false;
                }
            } catch (Throwable ex) {
                Logger.warn("BeforeNotify threw an Exception", ex);
            }
        }

        // By default, allow the error to be sent if there were no objections
        return true;
    }
}
