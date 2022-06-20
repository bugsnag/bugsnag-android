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
// - When our SIGQUIT handler triggers, we block SIGQUIT (so that the Google
//   handler can run), then trigger another "worker" thread we have waiting
//   to do the actual work, then return. The context switch to the "worker"
//   thread debounces the signaling mechanism so that we can trigger the Google
//   ANR thread safely. Our SIGQUIT handler MUST NOT run for more than 20
//   seconds or else we'll be force-killed, which breaks Google reporting and
//   the ANR popup in some cases.
//
// - Our ANR "worker" thread waits for our trigger, calls bsg_google_anr_call()
//   to raise a SIGQUIT on Google's handler thread, and then does any extra
//   processing we want to happen (this thread doesn't require async-safety).
//
// - When everything is done, we unblock SIGQUIT again so that our handler will
//   trigger on the next ANR, and then put the "worker" thread back to sleep.
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

static inline uint64_t sigmask_for_signal(uint64_t sig) {
  return (((uint64_t)1) << (sig - 1));
}

static bool is_thread_signal_catcher_sigblk(pid_t tid) {
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

  // The signal catcher thread will not have SIGQUIT blocked
  return (sigblk & sigmask_for_signal(SIGQUIT)) == 0;
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
