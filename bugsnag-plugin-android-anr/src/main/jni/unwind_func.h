#ifndef UNWIND_FUNC_H
#define UNWIND_FUNC_H

#include "bsg_unwind.h"
#include <signal.h>
#include <sys/types.h>

#ifdef __cplusplus
extern "C" {
#endif

typedef ssize_t (*unwind_func)(
    bugsnag_stackframe stacktrace[BUGSNAG_FRAMES_MAX], siginfo_t *info,
    void *user_context);

#ifdef __cplusplus
}
#endif
#endif
