#ifndef BUGSNAG_SIGNAL_HANDLER_H
#define BUGSNAG_SIGNAL_HANDLER_H
/**
 * The signal handler captures the application environment at the time a fatal
 * signal is raised and serializes a report to disk for later analysis.
 *
 * To use, install the handler with a reference to a crash environment. This
 * context is used to store application state leading up to a fatal signal being
 * raised.
 *
 * Example usage:
 *
 *     // (initialize handler during app initialization)
 *     bsg_handler_install_signal(env);
 *
 * References:
 * * https://www.gnu.org/software/libc/manual/html_node/Signal-Handling.html
 * * sigaction(2), sigaltstack(2)
 */

#include "../utils/build.h"
#include "bugsnag_ndk.h"

#ifdef __cplusplus
extern "C" {
#endif

/**
 * Monitor for fatal signals, updating the crash context when detected, the
 * serialize to disk and invoke the previously-installed handler
 * @return true if monitoring started successfully
 */
bool bsg_handler_install_signal(bsg_environment *env);
/**
 * Stop monitoring for fatal exceptions and reinstall previously-installed
 * handlers
 */
void bsg_handler_uninstall_signal(void) __asyncsafe;

#ifdef __cplusplus
}
#endif
#endif
