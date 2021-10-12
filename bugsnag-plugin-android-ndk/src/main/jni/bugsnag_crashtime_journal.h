//
// Created by Karl Stenerud on 06.10.21.
//

#ifndef BUGSNAG_ANDROID_BUGSNAG_CRASHTIME_JOURNAL_H
#define BUGSNAG_ANDROID_BUGSNAG_CRASHTIME_JOURNAL_H

#include "bugsnag_ndk.h"

#ifdef __cplusplus
extern "C" {
#endif

/**
 * Initialise the crash-time journaling system. This must be called as early as
 * possible so that all structures will be ready for a C++ or signal-based crash
 * event.
 *
 * @param journal_path /path/to/myjournal.journal.crashtime
 * @return true if initialisation was successful.
 */
bool bsg_crashtime_journal_init(const char *journal_path);

/**
 * Store the contents of an event to the journal.
 *
 * @param event The event to store.
 * @return true if the event was successfully stored.
 */
bool bsg_crashtime_journal_store_event(const bugsnag_event *event);

#ifdef __cplusplus
}
#endif

#endif // BUGSNAG_ANDROID_BUGSNAG_CRASHTIME_JOURNAL_H
