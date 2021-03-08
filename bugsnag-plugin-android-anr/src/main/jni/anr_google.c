// File:   anr_google.c
// Author: Karl Stenerud
// Date:   January 13, 2021
//
// Before starting an app, the Android runtime library first sets up its own
// SIGQUIT handler (art/runtime/signal_catcher.cc, art/runtime/signal_set.h),
// which listens for SIGQUIT messages that the OS sends to it when it detects an
// ANR. The handler uses sigwait() instead of the more common sigaction()
// mechanism, which complicates things:
//
// - sigwait() only triggers when a signal becomes PENDING, which can only
//   happen if the signal is blocked on all threads. As soon as a signal
//   handler gets called, the signal is no longer pending. Also, an unblocked
//   signal never becomes pending.
//
// - Blocked signals will never reach a signal handler registered via
//   sigaction(), which is why the old BSG implementation had to unblock
//   SIGQUIT to work (while at the same time breaking the Google handler).
//
// - Registering multiple handlers via sigwait() doesn't work because only the
//   first registered handler gets called. Once the first handler runs, the
//   signal is no longer pending. And since the first handler in this case is
//   registered by the runtime library before our code even has a chance to
//   run, we'd never get called.
//
// - Even though the runtime library has callback hooks for ANRs
//   (art/runtime/runtime.cc), it's not a public API, so we can't use it.
//
// So we need a workaround. Calling raise() and signal() won't work, because the
// signal ends up getting routed incorrectly. The only way to successfully
// signal the runtime handler is by use of a syscall() of SYS_tgkill with
// SIGQUIT that targets the Google handler thread ID directly.
//
//
// How to work around it:
//
// - Unblock SIGQUIT (breaking the Google handler)
//
// - Register a SIGQUIT handler via sigaction()
//
// - Our handler callback blocks SIGQUIT (allowing the Google handler to work
//   again) and sets a flag like "should_report_anr"
//
// - We have another thread waiting on the "should_report_anr" flag, which then
//   calls bsg_google_anr_call() after a short delay to raise a SIGQUIT on
//   Google's handler thread. Re-raising the signal on the signal handler
//   thread won't work because the signaling system in the OS won't finish
//   updating the new blocking state until the current signal handler returns.
//
// - Do our handler stuff, delay a short while (like 2 seconds) for Google's
//   handlers to finish, then exit our watchdog thread. It's important that
//   our watchdog thread stops before the runtime's timeout (currently 20
//   seconds), or else we'll be force-killed, which breaks Google reporting
//   and the ANR popup in some cases.
//
//
// Functionality in this file:
//
// - bsg_google_anr_init (NOT async-safe): Find and save the Google ANR handler
//   thread ID and the process ID, which are needed to make the syscall.
//
// - bsg_google_anr_call (async-safe): Make the syscall to send a SIGQUIT
//   directly to the runtime library's ANR handler thread.

#include "anr_google.h"

#include <ctype.h>
#include <dirent.h>
#include <inttypes.h>
#include <stdlib.h>
#include <string.h>
#include <sys/syscall.h>
#include <unistd.h>

static pid_t process_id = -1;
static pid_t google_thread_id = -1;

static bool is_thread_named_signal_catcher(pid_t tid) {
  static const char *const SIGNAL_CATCHER_THREAD_NAME = "Signal Catcher";

  bool success = false;
  char buff[256];

  snprintf(buff, sizeof(buff), "/proc/%d/comm", tid);
  FILE *fp = fopen(buff, "r");
  if (fp == NULL) {
    return false;
  }

  if (fgets(buff, sizeof(buff), fp) != NULL) {
    success = strncmp(buff, SIGNAL_CATCHER_THREAD_NAME,
                      strlen(SIGNAL_CATCHER_THREAD_NAME)) == 0;
  }

  fclose(fp);
  return success;
}

static bool is_thread_signal_catcher_sigblk(pid_t tid) {
  static const uint64_t SIGNAL_CATCHER_THREAD_SIGBLK = 0x1000;
  static const char *SIGBLK_HEADER = "SigBlk:\t";
  const size_t SIGBLK_HEADER_LENGTH = strlen(SIGBLK_HEADER);

  char buff[256];
  uint64_t sigblk = 0;

  snprintf(buff, sizeof(buff), "/proc/%d/status", tid);
  FILE *fp = fopen(buff, "r");
  if (fp == NULL) {
    return false;
  }

  while (fgets(buff, sizeof(buff), fp) != NULL) {
    if (strncmp(buff, SIGBLK_HEADER, SIGBLK_HEADER_LENGTH) == 0) {
      sigblk = strtoull(buff + SIGBLK_HEADER_LENGTH, NULL, 16);
      break;
    }
  }
  fclose(fp);
  return sigblk == SIGNAL_CATCHER_THREAD_SIGBLK;
}

bool bsg_google_anr_init() {
  pid_t pid = getpid();
  pid_t tid = -1;

  char path[256];
  snprintf(path, sizeof(path), "/proc/%d/task", pid);
  DIR *dir = opendir(path);
  if (dir == NULL) {
    return false;
  }

  struct dirent *dent;
  while ((dent = readdir(dir)) != NULL) {
    if (dent->d_name[0] < '0' || dent->d_name[0] > '9') {
      continue;
    }

    tid = strtol(dent->d_name, NULL, 10);
    if (is_thread_named_signal_catcher(tid) &&
        is_thread_signal_catcher_sigblk(tid)) {
      break;
    }
    tid = -1;
  }
  closedir(dir);

  if (tid < 0) {
    return false;
  }

  google_thread_id = tid;
  process_id = pid;
  return true;
}

void bsg_google_anr_call() {
  if (process_id >= 0 && google_thread_id >= 0) {
    syscall(SYS_tgkill, process_id, google_thread_id, SIGQUIT);
  }
}
