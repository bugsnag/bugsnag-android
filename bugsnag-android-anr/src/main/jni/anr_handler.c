#include "anr_handler.h"
#include <errno.h>
#include <pthread.h>
#include <signal.h>
#include <string.h>
#include <android/log.h>

#include "utils/string.h"

#ifndef BUGSNAG_LOG
#define BUGSNAG_LOG(fmt, ...)                                                  \
  __android_log_print(ANDROID_LOG_WARN, "BugsnagNDK", fmt, ##__VA_ARGS__)
#endif

static pthread_t bsg_watchdog_thread;
static sigset_t bsg_anr_sigmask;
static char *bsg_anr_indicator_buffer = NULL;

/* The previous SIGQUIT handler */
struct sigaction bsg_sigquit_sigaction_previous;

/* Write a sentinel value to the shared memory buffer to allow
 * detection of ANR state from JVM layer
 */
void bsg_handle_sigquit(int signum, siginfo_t *info, void *user_context) {
  static char *const anr_indicator = "a";
  if (bsg_anr_indicator_buffer != NULL) {
    bsg_strncpy(bsg_anr_indicator_buffer, anr_indicator, 2);
  }
  // Invoke previous handler, if a custom handler has been set
  struct sigaction previous = bsg_sigquit_sigaction_previous;
  if (previous.sa_flags & SA_SIGINFO) {
    previous.sa_sigaction(SIGQUIT, info, user_context);
  } else if (previous.sa_handler == SIG_DFL) {
    // Do nothing, the default action is nothing
  } else if (previous.sa_handler != SIG_IGN) {
    void (*previous_handler)(int) = previous.sa_handler;
    previous_handler(signum);
  }
}

/**
 * Configure ANR detection using a signal handler listening for SIGQUIT
 */
void *bsg_monitor_anrs(void *_arg) {
  struct sigaction handler;
  sigemptyset(&handler.sa_mask);
  handler.sa_sigaction = bsg_handle_sigquit;
  handler.sa_flags = SA_SIGINFO | SA_ONSTACK;
  int success = sigaction(SIGQUIT, &handler, &bsg_sigquit_sigaction_previous);
  if (success != 0) {
    BUGSNAG_LOG("Failed to install SIGQUIT handler: %s", strerror(errno));
  }

  return NULL;
}

bool bsg_handler_install_anr(void *byte_buffer) {
  static pthread_mutex_t bsg_anr_handler_config = PTHREAD_MUTEX_INITIALIZER;
  pthread_mutex_lock(&bsg_anr_handler_config);
  if (bsg_anr_indicator_buffer != NULL) {
    // Handler already configured
    pthread_mutex_unlock(&bsg_anr_handler_config);
    return true;
  }
  bsg_anr_indicator_buffer = byte_buffer;

  sigemptyset(&bsg_anr_sigmask);
  sigaddset(&bsg_anr_sigmask, SIGQUIT);

  int mask_status = pthread_sigmask(SIG_SETMASK, &bsg_anr_sigmask, NULL);
  if (mask_status != 0) {
    BUGSNAG_LOG("Failed to mask SIGQUIT: %s", strerror(mask_status));
  } else {
    pthread_create(&bsg_watchdog_thread, NULL, bsg_monitor_anrs, NULL);
    // unblock the current thread
    pthread_sigmask(SIG_UNBLOCK, &bsg_anr_sigmask, NULL);
  }

  pthread_mutex_unlock(&bsg_anr_handler_config);

  return true;
}

void bsg_handler_uninstall_anr() {
  if (bsg_anr_indicator_buffer == NULL) {
    return;
  }
  bsg_anr_indicator_buffer = NULL;
  sigaction(SIGQUIT, &bsg_sigquit_sigaction_previous, NULL);
  pthread_kill(bsg_watchdog_thread, SIGKILL);
}
