#include "signal_handler.h"

#include <bugsnag_ndk.h>
#include <errno.h>
#include <event.h>
#include <fcntl.h>
#include <pthread.h>
#include <signal.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>

#include "../utils/crash_info.h"
#include "../utils/serializer.h"
#include "../utils/string.h"
#include "../utils/threads.h"
#define BSG_HANDLED_SIGNAL_COUNT 6
#define BSG_SIGNAL_CODE_COUNT 15
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

static const char bsg_native_signal_code_names[BSG_HANDLED_SIGNAL_COUNT +
                                               1][BSG_SIGNAL_CODE_COUNT +
                                                  1][67] = {
    {"Illegal instruction, code 1 (ILLOPC)",
     "Illegal instruction, code 2 (ILLOPN)",
     "Illegal instruction, code 3 (ILLADR)",
     "Illegal instruction, code 4 (ILLTRP)",
     "Illegal instruction, code 5 (PRVOPC)",
     "Illegal instruction, code 6 (PRVREG)",
     "Illegal instruction, code 7 (COPROC)",
     "Illegal instruction, code 8 (BADSTK)",
     "Illegal instruction, code 9 (BADIADDR)",
     "Illegal instruction, code 10 (BREAK)",
     "Illegal instruction, code 11 (BNDMOD)"},
    {"Trace/breakpoint trap, code 1 (BRKPT)",
     "Trace/breakpoint trap, code 2 (TRACE)",
     "Trace/breakpoint trap, code 3 (BRANCH)",
     "Trace/breakpoint trap, code 4 (HWBKPT)",
     "Trace/breakpoint trap, code 5 (UNK)",
     "Trace/breakpoint trap, code 6 (PERF)"},
    {0},
    {"Bus error (bad memory access), code 1 (ADRALN)",
     "Bus error (bad memory access), code 2 (ADRERR)",
     "Bus error (bad memory access), code 3 (OBJERR)",
     "Bus error (bad memory access), code 4 (MCEERR_AR)",
     "Bus error (bad memory access), code 5 (MCEERR_AO)"},

    {"Floating-point exception, code 1 (INTDIV)",
     "Floating-point exception, code 2 (INTOVF)",
     "Floating-point exception, code 3 (FLTDIV)",
     "Floating-point exception, code 4 (FLTOVF)",
     "Floating-point exception, code 5 (FLTUND)",
     "Floating-point exception, code 6 (FLTRES)",
     "Floating-point exception, code 7 (FLTINV)",
     "Floating-point exception, code 8 (FLTSUB)",
     "Floating-point exception, code 9 (DECOVF)",
     "Floating-point exception, code 10 (DECDIV)",
     "Floating-point exception, code 11 (DECERR)",
     "Floating-point exception, code 12 (INVASC)",
     "Floating-point exception, code 13 (INVDEC)",
     "Floating-point exception, code 14 (FLTUNK)",
     "Floating-point exception, code 15 (CONDTRAP)"},
    {"Segmentation violation (invalid memory reference), code 1 (MAPERR)",
     "Segmentation violation (invalid memory reference), code 2 (ACCERR)",
     "Segmentation violation (invalid memory reference), code 3 (BNDERR)",
     "Segmentation violation (invalid memory reference), code 4 (PKUERR)",
     "Segmentation violation (invalid memory reference), code 5 (ACCADI)",
     "Segmentation violation (invalid memory reference), code 6 (ADIDERR)",
     "Segmentation violation (invalid memory reference), code 7 (ADIPERR)",
     "Segmentation violation (invalid memory reference), code 8 (MTEAERR)",
     "Segmentation violation (invalid memory reference), code 9 (MTESERR)"}};

static const int bsg_native_signal_codes[BSG_HANDLED_SIGNAL_COUNT +
                                         1][BSG_SIGNAL_CODE_COUNT + 1] = {
    {ILL_ILLOPC, ILL_ILLOPN, ILL_ILLADR, ILL_ILLTRP, ILL_PRVOPC, ILL_PRVREG,
     ILL_COPROC, ILL_BADSTK, ILL_BADIADDR, __ILL_BREAK, __ILL_BNDMOD},
    {TRAP_BRKPT, TRAP_TRACE, TRAP_BRANCH, TRAP_HWBKPT, TRAP_UNK, TRAP_PERF},
    {BUS_ADRALN, BUS_ADRERR, BUS_OBJERR, BUS_MCEERR_AR, BUS_MCEERR_AO},
    {FPE_INTDIV, FPE_INTOVF, FPE_FLTDIV, FPE_FLTOVF, FPE_FLTUND, FPE_FLTRES,
     FPE_FLTINV, FPE_FLTSUB, __FPE_DECOVF, __FPE_DECDIV, __FPE_DECERR,
     __FPE_INVASC, __FPE_INVDEC, FPE_FLTUNK, FPE_CONDTRAP},
    {SEGV_MAPERR, SEGV_ACCERR, SEGV_BNDERR, SEGV_PKUERR, SEGV_ACCADI,
     SEGV_ADIDERR, SEGV_ADIPERR, SEGV_MTEAERR, SEGV_MTESERR}};

const char *bsg_get_signal_code_description(const int signal,
                                            const int signal_code) __asyncsafe {
  for (int i = 0; i < BSG_HANDLED_SIGNAL_COUNT; i++) {
    if (bsg_native_signals[i] == signal) {
      for (int j = 0; j < BSG_SIGNAL_CODE_COUNT; j++) {
        printf("%s", "aaaaaaaaaaaaaaa,signal");
        printf("%d", signal);
        if (bsg_native_signal_codes[i][j] == signal_code) {
          return bsg_native_signal_code_names[i][j];
          printf("%s", "aaaaaaaaaaaaaaa,signal description");
          printf("%s", bsg_native_signal_code_names[i][j]);
        }
      }
    }
  }
  return NULL;
}

bool bsg_handler_install_signal(bsg_environment *env) {
  if (bsg_global_env != NULL) {
    return true; // already installed
  }
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
  if (bsg_global_env == NULL) {
    return;
  }
  if (!bsg_begin_handling_crash()) {
    return;
  }

  if (bsg_global_env->crash_handled) {
    // The C++ handler default action is to raise a fatal signal once
    // handling is complete. The report is already generated so at this
    // point, the handler only needs to be uninstalled.
    bsg_handler_uninstall_signal();
    bsg_invoke_previous_signal_handler(signum, info, user_context);
    return;
  }

  bsg_global_env->next_event.unhandled = true;
  bsg_populate_event_as(bsg_global_env);
  bsg_global_env->next_event.error.frame_count = bsg_unwind_crash_stack(
      bsg_global_env->next_event.error.stacktrace, info, user_context);

  if (bsg_global_env->send_threads != SEND_THREADS_NEVER) {
    bsg_global_env->next_event.thread_count = bsg_capture_thread_states(
        gettid(), bsg_global_env->next_event.threads, BUGSNAG_THREADS_MAX);
  } else {
    bsg_global_env->next_event.thread_count = 0;
  }

  for (int i = 0; i < BSG_HANDLED_SIGNAL_COUNT; i++) {
    const int signal = bsg_native_signals[i];
    const int signal_code = info->si_code;
    if (signal == signum) {
      bsg_strncpy(bsg_global_env->next_event.error.errorClass,
                  (char *)bsg_native_signal_names[i],
                  sizeof(bsg_global_env->next_event.error.errorClass));
      const char *error_message =
          bsg_get_signal_code_description(signal, signal_code);
      if (error_message == NULL || *error_message == 0) {
        error_message = (char *)bsg_native_signal_msgs[i];
        printf("%s", "aaaaaaaaaaaaaaa, em is null or 0");

      } else {
        printf("%s", "aaaaaaaaaaaaaaa");
        printf("%s", error_message);
        printf("%d", signal_code);
      }
      bsg_strncpy(bsg_global_env->next_event.error.errorMessage, error_message,
                  sizeof(bsg_global_env->next_event.error.errorMessage));
      break;
    }
  }
  if (bsg_run_on_error()) {
    bsg_increment_unhandled_count(&bsg_global_env->next_event);
    bsg_serialize_event_to_file(bsg_global_env);
    bsg_serialize_last_run_info_to_file(bsg_global_env);
  }

  bsg_finish_handling_crash();
  bsg_handler_uninstall_signal();
  bsg_invoke_previous_signal_handler(signum, info, user_context);
}
