//
// Created by Karl Stenerud on 06.10.21.
//

#ifndef BUGSNAG_ANDROID_BUGSNAG_CRASHTIME_JOURNAL_H
#define BUGSNAG_ANDROID_BUGSNAG_CRASHTIME_JOURNAL_H

#include "bugsnag_ndk.h"

#ifdef __cplusplus
extern "C" {
#endif

bool bsg_crashtime_journal_init(const char *journal_path);

bool bsg_crashtime_journal_store_event(const bugsnag_event *event);

#ifdef __cplusplus
}
#endif

#endif // BUGSNAG_ANDROID_BUGSNAG_CRASHTIME_JOURNAL_H
