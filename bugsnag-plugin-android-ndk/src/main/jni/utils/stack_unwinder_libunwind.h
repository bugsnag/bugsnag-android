#ifndef BUGSNAG_UTILS_STACK_UNWINDER_LIBUNWIND_H
#define BUGSNAG_UTILS_STACK_UNWINDER_LIBUNWIND_H

#include "event.h"
#include <signal.h>

bool bsg_configure_libunwind(bool is32bit);

ssize_t
bsg_unwind_stack_libunwind(bsg_stackframe stacktrace[BUGSNAG_FRAMES_MAX],
                           siginfo_t *info, void *user_context);

#endif
