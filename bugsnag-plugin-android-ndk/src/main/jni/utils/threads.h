#ifndef BUGSNAG_THREADS_H
#define BUGSNAG_THREADS_H

#include "../event.h"
#include "build.h"

#ifdef __cplusplus
extern "C" {
#endif

#define MAX_STAT_PATH_LENGTH 64

size_t bsg_capture_thread_states(pid_t reporting_tid, bsg_thread *threads,
                                 size_t max_threads) __asyncsafe;

#ifdef __cplusplus
}
#endif
#endif // BUGSNAG_THREADS_H
