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

#define MSG_SIGILL "Illegal instruction"
#define MSG_SIGTRAP "Trace/breakpoint trap"
#define MSG_SIGABRT "Abort program"
#define MSG_SIGBUS "Bus error (bad memory access)"
#define MSG_SIGFPE "Floating-point exception"
#define MSG_SIGSEGV "Segmentation violation (invalid memory reference)"

#define xstr(s) str(s)
#define str(s) #s
#define SIG_CODE_MESSAGE(msg, code) (msg ", code " xstr(code) " (" #code ")")

/**
 * Native signals which will be captured by the Bugsnag signal handler™
 */
static const int bsg_native_signals[BSG_HANDLED_SIGNAL_COUNT + 1] = {
    SIGILL, SIGTRAP, SIGABRT, SIGBUS, SIGFPE, SIGSEGV};
static const char bsg_native_signal_names[BSG_HANDLED_SIGNAL_COUNT + 1][8] = {
    "SIGILL", "SIGTRAP", "SIGABRT", "SIGBUS", "SIGFPE", "SIGSEGV"};
static const char bsg_native_signal_msgs[BSG_HANDLED_SIGNAL_COUNT + 1][60] = {
    MSG_SIGILL, MSG_SIGTRAP, MSG_SIGABRT, MSG_SIGBUS, MSG_SIGFPE, MSG_SIGSEGV};

static const char
    bsg_native_signal_code_names[BSG_HANDLED_SIGNAL_COUNT +
                                 1][BSG_SIGNAL_CODE_COUNT + 1][72] = {
        {SIG_CODE_MESSAGE(MSG_SIGILL, ILL_ILLOPC),
         SIG_CODE_MESSAGE(MSG_SIGILL, ILL_ILLOPN),
         SIG_CODE_MESSAGE(MSG_SIGILL, ILL_ILLADR),
         SIG_CODE_MESSAGE(MSG_SIGILL, ILL_ILLTRP),
         SIG_CODE_MESSAGE(MSG_SIGILL, ILL_PRVOPC),
         SIG_CODE_MESSAGE(MSG_SIGILL, ILL_PRVREG),
         SIG_CODE_MESSAGE(MSG_SIGILL, ILL_COPROC),
         SIG_CODE_MESSAGE(MSG_SIGILL, ILL_BADSTK),
         SIG_CODE_MESSAGE(MSG_SIGILL, ILL_BADIADDR),
         SIG_CODE_MESSAGE(MSG_SIGILL, __ILL_BREAK),
         SIG_CODE_MESSAGE(MSG_SIGILL, __ILL_BNDMOD)},
        {SIG_CODE_MESSAGE(MSG_SIGTRAP, TRAP_BRKPT),
         SIG_CODE_MESSAGE(MSG_SIGTRAP, TRAP_TRACE),
         SIG_CODE_MESSAGE(MSG_SIGTRAP, TRAP_BRANCH),
         SIG_CODE_MESSAGE(MSG_SIGTRAP, TRAP_HWBKPT),
         SIG_CODE_MESSAGE(MSG_SIGTRAP, TRAP_UNK),
         SIG_CODE_MESSAGE(MSG_SIGTRAP, TRAP_PERF)},
        {0},
        {SIG_CODE_MESSAGE(MSG_SIGBUS, BUS_ADRALN),
         SIG_CODE_MESSAGE(MSG_SIGBUS, BUS_ADRERR),
         SIG_CODE_MESSAGE(MSG_SIGBUS, BUS_OBJERR),
         SIG_CODE_MESSAGE(MSG_SIGBUS, BUS_MCEERR_AR),
         SIG_CODE_MESSAGE(MSG_SIGBUS, BUS_MCEERR_AO)},
        {SIG_CODE_MESSAGE(MSG_SIGFPE, FPE_INTDIV),
         SIG_CODE_MESSAGE(MSG_SIGFPE, FPE_INTOVF),
         SIG_CODE_MESSAGE(MSG_SIGFPE, FPE_FLTDIV),
         SIG_CODE_MESSAGE(MSG_SIGFPE, FPE_FLTOVF),
         SIG_CODE_MESSAGE(MSG_SIGFPE, FPE_FLTUND),
         SIG_CODE_MESSAGE(MSG_SIGFPE, FPE_FLTRES),
         SIG_CODE_MESSAGE(MSG_SIGFPE, FPE_FLTINV),
         SIG_CODE_MESSAGE(MSG_SIGFPE, FPE_FLTSUB),
         SIG_CODE_MESSAGE(MSG_SIGFPE, __FPE_DECOVF),
         SIG_CODE_MESSAGE(MSG_SIGFPE, __FPE_DECDIV),
         SIG_CODE_MESSAGE(MSG_SIGFPE, __FPE_DECERR),
         SIG_CODE_MESSAGE(MSG_SIGFPE, __FPE_INVASC),
         SIG_CODE_MESSAGE(MSG_SIGFPE, __FPE_INVDEC),
         SIG_CODE_MESSAGE(MSG_SIGFPE, FPE_FLTUNK),
         SIG_CODE_MESSAGE(MSG_SIGFPE, FPE_CONDTRAP)},
        {SIG_CODE_MESSAGE(MSG_SIGSEGV, SEGV_MAPERR),
         SIG_CODE_MESSAGE(MSG_SIGSEGV, SEGV_ACCERR),
         SIG_CODE_MESSAGE(MSG_SIGSEGV, SEGV_BNDERR),
         SIG_CODE_MESSAGE(MSG_SIGSEGV, SEGV_PKUERR),
         SIG_CODE_MESSAGE(MSG_SIGSEGV, SEGV_ACCADI),
         SIG_CODE_MESSAGE(MSG_SIGSEGV, SEGV_ADIDERR),
         SIG_CODE_MESSAGE(MSG_SIGSEGV, SEGV_ADIPERR),
         SIG_CODE_MESSAGE(MSG_SIGSEGV, SEGV_MTEAERR),
         SIG_CODE_MESSAGE(MSG_SIGSEGV, SEGV_MTESERR)}};

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

static const char *
bsg_get_signal_code_description(const int signal,
                                const int signal_code) __asyncsafe {
  for (int i = 0; i < BSG_HANDLED_SIGNAL_COUNT; i++) {
    if (bsg_native_signals[i] == signal) {
      for (int j = 0; j < BSG_SIGNAL_CODE_COUNT; j++) {
        if (bsg_native_signal_codes[i][j] == signal_code) {
          return bsg_native_signal_code_names[i][j];
        } else if (*bsg_native_signal_code_names[i][j] == 0) {
          // NULL in the signal_code_name array indicates no more known codes
          break;
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
