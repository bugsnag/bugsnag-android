#ifndef BUGSNAG_ANR_GOOGLE_H
#define BUGSNAG_ANR_GOOGLE_H

#include <stdbool.h>

/**
 * Initialize the Google ANR caller. This must be called before any other
 * functions in this file. If the call fails, all other calls in this file will
 * be no-ops.
 *
 * Note: This function is NOT async-safe.
 *
 * @return true if successful.
 */
bool bsg_google_anr_init(void);

/**
 * Raises a SIGQUIT signal directly on Google's "Signal Catcher" thread in the
 * runtime library. This function will no-op if bsg_google_anr_init() was not
 * called, or the call failed.
 *
 * Note: This MUST be called from a non-signal-handler thread (create a separate
 * thread that waits to call this function).
 *
 * Note: This function is async-safe.
 */
void bsg_google_anr_call(void);

#endif
