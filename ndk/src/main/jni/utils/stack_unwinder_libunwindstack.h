#ifndef BUGSNAG_UTILS_STACK_UNWINDER_LIBUNWINDSTACK_H
#define BUGSNAG_UTILS_STACK_UNWINDER_LIBUNWINDSTACK_H

#include "../report.h"
#include <signal.h>

#ifdef __cplusplus
extern "C"
#endif
ssize_t
bsg_unwind_stack_libunwindstack(bsg_stackframe stacktrace[BUGSNAG_FRAMES_MAX],
                                siginfo_t *info, void *user_context);
#endif
