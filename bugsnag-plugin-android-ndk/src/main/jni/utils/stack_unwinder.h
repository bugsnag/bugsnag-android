#ifndef BSG_STACK_UNWINDER_H
#define BSG_STACK_UNWINDER_H

#include <event.h>
#include <signal.h>
#include "build.h"

#ifdef __cplusplus
extern "C" {
#endif

typedef enum {
  BSG_LIBUNWIND,
  BSG_LIBUNWINDSTACK,
  BSG_LIBCORKSCREW,
  BSG_CUSTOM_UNWIND,
} bsg_unwinder;


/**
 * Based on the current environment, determine what unwinding library to use.
 *
 * Android API level 21+: libunwind
 * Android API level 16-19: libunwind, unless in a signal handler. Then
 * libcorkscrew.
 * Everything else: custom unwinding logic
 */
void bsg_set_unwind_types(int apiLevel, bool is32bit,
                          bsg_unwinder *signal_type,
                          bsg_unwinder *other_type);

/**
 * Unwind the stack using the preferred tool/style. If info and a user
 * context pointer are provided, the exception stack will be walked. Otherwise,
 * the current stack will be walked instead. The results will populate the
 * stacktrace
 * @return the number of frames
 */
ssize_t bsg_unwind_stack(bsg_unwinder unwind_style,
                     bsg_stackframe_t stacktrace[BUGSNAG_FRAMES_MAX],
                         siginfo_t *info, void *user_context) __asyncsafe;

#ifdef __cplusplus
}
#endif
#endif
