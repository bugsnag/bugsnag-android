#ifndef BUGSNAG_CPP_HANDLER_H
#define BUGSNAG_CPP_HANDLER_H
/**
 * The C++ handler captures the application environment at the time an uncaught
 * invocation to `throw` propagates to the termination handler and serializes
 * a report for later analysis.
 *
 * Example usage:
 *
 *     // (initialize handler during app initialization)
 *     bsg_handler_install_cpp(env);
 *
 * References:
 * * https://docs.microsoft.com/en-us/cpp/c-runtime-library/reference/set-terminate-crt
 */
#include "bugsnag_ndk.h"
#ifdef __cplusplus
extern "C" {
#endif

/**
 * Monitor for uncaught exceptions, updating the crash context when detected,
 * the serialize to disk and invoke the previously-installed handler @return
 * true if monitoring started successfully.
 */
bool bsg_handler_install_cpp(bsg_environment *env);
/**
 * Stop monitoring for uncaught C++ exceptions and reinstall
 * previously-installed handlers
 */
void bsg_handler_uninstall_cpp(void);

#ifdef __cplusplus
}
#endif
#endif
