#pragma once

#include "build.h"
#include <event.h>
#include <signal.h>

#ifdef __cplusplus
extern "C" {
#endif

/**
 * Initialize the stack unwinder. Must be called prior to initial use.
 */
void bsg_unwinder_init(void);

/**
 * Refresh the stack unwinder. This can be called to force a refresh of any
 * cached data within the unwinder.
 */
void bsg_unwinder_refresh(void);

/**
 * Unwind a stack in a terminating context. If info and a user context pointer
 * are provided, the exception stack will be walked. Otherwise, the current
 * stack will be walked. The results will populate the stack.
 *
 * @param stack        buffer to contain the frame contents
 * @param info         signal info or null if none
 * @param user_context crash context or null if none
 *
 * @return the number of frames
 */
ssize_t bsg_unwind_crash_stack(bugsnag_stackframe stack[BUGSNAG_FRAMES_MAX],
                               siginfo_t *info, void *user_context) __asyncsafe;

/**
 * Unwind a stack in a thread-safe context. If info and a user context pointer
 * are provided, the exception stack will be walked. Otherwise, the current
 * stack will be walked. The results will populate the stack.
 *
 * @param stack        buffer to contain the frame contents
 * @param info         IGNORED - provided for signature compatibility
 * @param user_context IGNORED - provided for signature compatibility
 *
 * @return the number of frames
 */
ssize_t
bsg_unwind_concurrent_stack(bugsnag_stackframe stack[BUGSNAG_FRAMES_MAX],
                            siginfo_t *info, void *user_context);

#ifdef __cplusplus
}
#endif
