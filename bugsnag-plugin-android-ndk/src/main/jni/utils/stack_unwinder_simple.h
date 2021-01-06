#ifndef BUGSNAG_UTILS_STACK_UNWINDER_SIMPLE_H
#define BUGSNAG_UTILS_STACK_UNWINDER_SIMPLE_H

#include "../event.h"
#include <signal.h>

ssize_t
bsg_unwind_stack_simple(bugsnag_stackframe stacktrace[BUGSNAG_FRAMES_MAX],
                        siginfo_t *info, void *user_context);
#endif
