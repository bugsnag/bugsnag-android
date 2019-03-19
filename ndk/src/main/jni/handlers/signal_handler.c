#include "signal_handler.h"

#include <bugsnag_ndk.h>
#include <errno.h>
#include <fcntl.h>
#include <pthread.h>
#include <report.h>
#include <signal.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>

#include "../utils/crash_info.h"
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
static const char bsg_native_signal_msgs[BSG_HANDLED_SIGNAL_COUNT + 1][60] = {
    "Illegal instruction",
    "Trace/breakpoint trap",
    "Abort program",
    "Bus error (bad memory access)",
    "Floating-point exception",
    "Segmentation violation (invalid memory reference)"};

bool bsg_handler_install_signal(bsg_environment *env) {
  static pthread_mutex_t bsg_signal_handler_config = PTHREAD_MUTEX_INITIALIZER;
  pthread_mutex_lock(&bsg_signal_handler_config);
  if (!bsg_configure_signal_stack()) {
    pthread_mutex_unlock(&bsg_signal_handler_config);
    return false;
  }

  bsg_global_env = env;
  bsg_global_sigaction =
      calloc(sizeof(struct sigaction), BSG_HANDLED_SIGNAL_COUNT);
  if (bsg_global_sigaction == NULL) {
    pthread_mutex_unlock(&bsg_signal_handler_config);
    return false;
  }
  sigemptyset(&bsg_global_sigaction->sa_mask);
  bsg_global_sigaction->sa_sigaction = bsg_handle_signal;
  bsg_global_sigaction->sa_flags = SA_SIGINFO | SA_ONSTACK;

  bsg_global_sigaction_previous =
      calloc(sizeof(struct sigaction), BSG_HANDLED_SIGNAL_COUNT);
  if (bsg_global_sigaction_previous == NULL) {
    pthread_mutex_unlock(&bsg_signal_handler_config);
    return false;
  }
  for (int i = 0; i < BSG_HANDLED_SIGNAL_COUNT; i++) {
    const int signal = bsg_native_signals[i];
    int success = sigaction(signal, bsg_global_sigaction,
                            &bsg_global_sigaction_previous[i]);
    if (success != 0) {
      BUGSNAG_LOG("Failed to install signal handler: %s", strerror(errno));
      pthread_mutex_unlock(&bsg_signal_handler_config);
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
  static size_t bsg_stack_size = SIGSTKSZ * 2;
  if ((bsg_global_signal_stack.ss_sp = calloc(1, bsg_stack_size)) == NULL) {
    BUGSNAG_LOG(
        "Failed to allocate a alternate stack (%udKiB) for unwinding signals",
        (unsigned int)bsg_stack_size);
    return false;
  }
  bsg_global_signal_stack.ss_size = bsg_stack_size;
  bsg_global_signal_stack.ss_flags = 0;
  if (sigaltstack(&bsg_global_signal_stack, 0) < 0) {
    BUGSNAG_LOG("Failed to configure alt stack: %s", strerror(errno));
    return false;
  }
  return true;
}

void bsg_invoke_previous_signal_handler(int signum, siginfo_t *info,
                                        void *user_context) __asyncsafe {
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

void bsg_handle_signal(int signum, siginfo_t *info,
                       void *user_context) __asyncsafe {
  if (bsg_global_env == NULL || bsg_global_env->handling_crash) {
    if (bsg_global_env->crash_handled) {
      // The C++ handler default action is to raise a fatal signal once
      // handling is complete. The report is already generated so at this
      // point, the handler only needs to be uninstalled.
      bsg_handler_uninstall_signal();
      bsg_invoke_previous_signal_handler(signum, info, user_context);
    }
    return;
  }

  bsg_global_env->handling_crash = true;
  bsg_populate_report_as(bsg_global_env);
  bsg_global_env->next_report.unhandled_events++;
  bsg_global_env->next_report.exception.frame_count = bsg_unwind_stack(
      bsg_global_env->signal_unwind_style,
      bsg_global_env->next_report.exception.stacktrace, info, user_context);

  for (int i = 0; i < BSG_HANDLED_SIGNAL_COUNT; i++) {
    const int signal = bsg_native_signals[i];
    if (signal == signum) {
      bsg_strcpy(bsg_global_env->next_report.exception.name,
                 (char *)bsg_native_signal_names[i]);
      bsg_strcpy(bsg_global_env->next_report.exception.message,
                 (char *)bsg_native_signal_msgs[i]);
      break;
    }
  }
  bsg_serialize_report_to_file(bsg_global_env);
  bsg_handler_uninstall_signal();
  bsg_invoke_previous_signal_handler(signum, info, user_context);
}
