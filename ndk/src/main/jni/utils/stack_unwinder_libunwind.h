#ifndef BUGSNAG_UTILS_STACK_UNWINDER_LIBUNWIND_H
#define BUGSNAG_UTILS_STACK_UNWINDER_LIBUNWIND_H

#include "../report.h"
#include <signal.h>

bool bsg_configure_libunwind(void);

ssize_t
bsg_unwind_stack_libunwind(bsg_stackframe stacktrace[BUGSNAG_FRAMES_MAX],
                           siginfo_t *info, void *user_context);

#endif
