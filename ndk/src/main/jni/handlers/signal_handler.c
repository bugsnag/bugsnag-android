#include "signal_handler.h"

#include <bugsnag_ndk.h>
#include <fcntl.h>
#include <pthread.h>
#include <report.h>
#include <signal.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>

#include "../utils/serializer.h"
#include "../utils/string.h"
#define BSG_HANDLED_SIGNAL_COUNT 6

/**
 * Function to capture signals and write reports to disk
 * @param signum The captured signal number
 * @param info handler info, flags
 * @param user_context never gaze too deep into the void star for it will gaze
 * back into you
 */
void bsg_handle_signal(int signum, siginfo_t *info,
                       void *user_context) __asyncsafe;

/**
 * Allocate and configure a separate stack for handling signals. It will be
 * tiny!
 * @return true if successful
 */
bool bsg_configure_signal_stack(void);

/**
 * Reference to stack used to handle signals
 */
stack_t bsg_global_signal_stack;
/**
 * Global shared context for Bugsnag reports
 */
static bsg_environment *bsg_global_env;
/* the Bugsnag signal handler array */
struct sigaction *bsg_global_sigaction;

/* the previous signal handler array */
struct sigaction *bsg_global_sigaction_previous;

/**
 * Native signals which will be captured by the Bugsnag signal handler™
 */
static const int bsg_native_signals[BSG_HANDLED_SIGNAL_COUNT + 1] = {
    SIGILL, SIGTRAP, SIGABRT, SIGBUS, SIGFPE, SIGSEGV};
static const char bsg_native_signal_names[BSG_HANDLED_SIGNAL_COUNT + 1][8] = {
    "SIGILL", "SIGTRAP", "SIGABRT", "SIGBUS", "SIGFPE", "SIGSEGV"};

bool bsg_handler_install_signal(bsg_environment *env) {
  static pthread_mutex_t bsg_signal_handler_config = PTHREAD_MUTEX_INITIALIZER;
  pthread_mutex_lock(&bsg_signal_handler_config);
  if (!bsg_configure_signal_stack()) {
    return false;
  }

  bsg_global_env = env;
  bsg_global_sigaction =
      calloc(sizeof(struct sigaction), BSG_HANDLED_SIGNAL_COUNT);
  if (bsg_global_sigaction == NULL) {
    return false;
  }
  sigemptyset(&bsg_global_sigaction->sa_mask);
  bsg_global_sigaction->sa_sigaction = bsg_handle_signal;
  bsg_global_sigaction->sa_flags = SA_SIGINFO | SA_ONSTACK;

  bsg_global_sigaction_previous =
      calloc(sizeof(struct sigaction), BSG_HANDLED_SIGNAL_COUNT);
  if (bsg_global_sigaction_previous == NULL) {
    return false;
  }
  for (int i = 0; i < BSG_HANDLED_SIGNAL_COUNT; i++) {
    const int signal = bsg_native_signals[i];
    int success = sigaction(signal, bsg_global_sigaction,
                            &bsg_global_sigaction_previous[i]);
    if (success != 0) {
      // TODO: log errno in this case
      return false;
    }
  }

  pthread_mutex_unlock(&bsg_signal_handler_config);

  return true;
}

void bsg_handler_uninstall_signal() {
  if (bsg_global_env == NULL)
    return;
  for (int i = 0; i < BSG_HANDLED_SIGNAL_COUNT; i++) {
    const int signal = bsg_native_signals[i];
    sigaction(signal, &bsg_global_sigaction_previous[i], 0);
  }
  bsg_global_env = NULL;
}

bool bsg_configure_signal_stack() {
  if ((bsg_global_signal_stack.ss_sp = malloc(SIGSTKSZ)) == NULL) {
    // Failed to allocate a alternate stack for unwinding signals
    return false;
  }
  bsg_global_signal_stack.ss_size = SIGSTKSZ;
  bsg_global_signal_stack.ss_flags = 0;
  if (sigaltstack(&bsg_global_signal_stack, 0) < 0) {
    // TODO: log errno in this case
    return false;
  }
  return true;
}

void bsg_handle_signal(int signum, siginfo_t *info,
                       void *user_context) __asyncsafe {
  static time_t now;
  if (bsg_global_env == NULL)
    return;

  bsg_global_env->next_report.device.time = time(&now);
  bsg_global_env->next_report.exception.frame_count = bsg_unwind_stack(
      bsg_global_env->unwind_style,
      bsg_global_env->next_report.exception.stacktrace, info, user_context);

  for (int i = 0; i < BSG_HANDLED_SIGNAL_COUNT; i++) {
    const int signal = bsg_native_signals[i];
    if (signal == signum) {
      bsg_strcpy(bsg_global_env->next_report.exception.name,
                 (char *)bsg_native_signal_names[i]);
      break;
    }
  }
  // TODO: handle failure to serialize report with error logging
  bsg_serialize_report_to_file(bsg_global_env);
  bsg_handler_uninstall_signal();
  for (int i = 0; i < BSG_HANDLED_SIGNAL_COUNT; ++i) {
    const int signal = bsg_native_signals[i];
    if (signal == signum) {
      struct sigaction previous = bsg_global_sigaction_previous[i];
      // From sigaction(2):
      //  > If act is non-zero, it specifies an action (SIG_DFL, SIG_IGN, or a
      //  handler routine)
      if (previous.sa_flags & SA_SIGINFO) {
        // This handler can handle signal number, info, and user context
        // (POSIX). From sigaction(2): > If this bit is set, the handler
        // function is assumed to be pointed to by the sa_sigaction member of
        // struct sigaction and should match the proto- type shown above or as
        // below in EXAMPLES.  This bit should not be set when assigning SIG_DFL
        // or SIG_IGN.
        previous.sa_sigaction(signum, info, user_context);
      } else if (previous.sa_handler == SIG_DFL) {
        raise(signum); // raise to trigger the default handler. It cannot be
                       // called directly.
      } else if (previous.sa_handler != SIG_IGN) {
        // This handler can only handle to signal number (ANSI C)
        void (*previous_handler)(int) = previous.sa_handler;
        previous_handler(signum);
      }
    }
  }
}
