#include "anr_handler.h"
#include <android/log.h>
#include <errno.h>
#include <jni.h>
#include <pthread.h>
#include <semaphore.h>
#include <signal.h>
#include <string.h>
#include <unistd.h>

#include "anr_google.h"
#include "anr_jni_cache.h"
#include "anr_safejni.h"
#include "unwind_func.h"
#include "utils/string.h"

// Lock for changing the handler configuration
static pthread_mutex_t bsg_anr_handler_config = PTHREAD_MUTEX_INITIALIZER;

// A proxy for install/uninstall state, to avoid needing to unset the handler
// on the sigquit-watching thread
static bool enabled = false;
static bool installed = false;

static pthread_t watchdog_thread;
static bool should_wait_for_semaphore = false;
static sem_t watchdog_thread_semaphore;
static volatile bool watchdog_thread_triggered = false;

static bugsnag_stackframe anr_stacktrace[BUGSNAG_FRAMES_MAX];
static ssize_t anr_stacktrace_length = 0;

static unwind_func unwind_stack_function;

static jobject anr_plugin;

static void notify_anr_detected(JNIEnv *env) {
  if (!enabled || env == NULL || anr_plugin == NULL) {
    return;
  }

  jobject jlist = bsg_anr_safe_new_object(env, bsg_anr_jni_cache->LinkedList,
                                          bsg_anr_jni_cache->LinkedList_init);
  if (jlist == NULL) {
    return;
  }

  for (ssize_t i = 0; i < anr_stacktrace_length; i++) {
    bugsnag_stackframe *frame = anr_stacktrace + i;
    jobject jmethod = bsg_anr_safe_new_string_utf(env, frame->method);
    jobject jfilename = bsg_anr_safe_new_string_utf(env, frame->filename);
    jobject jline_number = bsg_anr_safe_new_object(
        env, bsg_anr_jni_cache->Integer, bsg_anr_jni_cache->Integer_init,
        (jint)frame->line_number);
    jobject jframe_address = bsg_anr_safe_new_object(
        env, bsg_anr_jni_cache->Long, bsg_anr_jni_cache->Long_init,
        (jlong)frame->frame_address);
    jobject jsymbol_address = bsg_anr_safe_new_object(
        env, bsg_anr_jni_cache->Long, bsg_anr_jni_cache->Long_init,
        (jlong)frame->symbol_address);
    jobject jload_address = bsg_anr_safe_new_object(
        env, bsg_anr_jni_cache->Long, bsg_anr_jni_cache->Long_init,
        (jlong)frame->load_address);
    jobject jframe = bsg_anr_safe_new_object(
        env, bsg_anr_jni_cache->NativeStackFrame,
        bsg_anr_jni_cache->NativeStackFrame_init, jmethod, jfilename,
        jline_number, jframe_address, jsymbol_address, jload_address, NULL,
        bsg_anr_jni_cache->ErrorType_C);
    if (jframe != NULL) {
      (*env)->CallBooleanMethod(env, jlist, bsg_anr_jni_cache->LinkedList_add,
                                jframe);
      bsg_anr_check_and_clear_exc(env);
    }
    bsg_anr_safe_delete_local_ref(env, jmethod);
    bsg_anr_safe_delete_local_ref(env, jfilename);
    bsg_anr_safe_delete_local_ref(env, jline_number);
    bsg_anr_safe_delete_local_ref(env, jframe_address);
    bsg_anr_safe_delete_local_ref(env, jsymbol_address);
    bsg_anr_safe_delete_local_ref(env, jload_address);
    bsg_anr_safe_delete_local_ref(env, jframe);
  }

  (*env)->CallVoidMethod(env, anr_plugin,
                         bsg_anr_jni_cache->AnrPlugin_notifyAnrDetected, jlist);
  bsg_anr_check_and_clear_exc(env);
  bsg_anr_safe_delete_local_ref(env, jlist);
}

static inline void block_sigquit() {
  sigset_t sigmask;
  sigemptyset(&sigmask);
  sigaddset(&sigmask, SIGQUIT);
  if (pthread_sigmask(SIG_BLOCK, &sigmask, NULL) != 0) {
    BUGSNAG_LOG(
        "Could not block SIGQUIT. Google's ANR handling will be disabled.");
  }
}

static inline void unblock_sigquit() {
  sigset_t anr_sigmask;
  sigemptyset(&anr_sigmask);
  sigaddset(&anr_sigmask, SIGQUIT);
  if (pthread_sigmask(SIG_UNBLOCK, &anr_sigmask, NULL) != 0) {
    BUGSNAG_LOG(
        "Could not unblock SIGQUIT. Bugsnag's ANR handling will be disabled.");
  }
}

static inline void trigger_sigquit_watchdog_thread() {
  // Set the trigger flag for the fallback spin-lock in
  // sigquit_watchdog_thread_main()
  watchdog_thread_triggered = true;

  if (should_wait_for_semaphore) {
    // Although sem_post() is not officially marked as async-safe, the Android
    // implementation simply does an atomic compare-and-exchange when there is
    // only one thread waiting (which is the case here).
    // https://cs.android.com/android/platform/superproject/+/master:bionic/libc/bionic/semaphore.cpp;l=289?q=sem_post&ss=android
    if (sem_post(&watchdog_thread_semaphore) != 0) {
      // The only possible failure from sem_post is EOVERFLOW, which won't
      // happen in this code. But just to be thorough...
      BUGSNAG_LOG("Could not unlock Bugsnag sigquit handler semaphore");
    }
  }
}

static void watchdog_wait_for_trigger() {
  static const useconds_t delay_100ms = 100000;

  // Use sem_wait() if possible, falling back to polling.
  watchdog_thread_triggered = false;
  if (!should_wait_for_semaphore || sem_wait(&watchdog_thread_semaphore) != 0) {
    while (!watchdog_thread_triggered) {
      usleep(delay_100ms);
    }
  }
}

_Noreturn static void *sigquit_watchdog_thread_main(__unused void *_) {
  static const useconds_t delay_100ms = 100000;

  // Hold on to the Java env for the duration of the app.
  JNIEnv *env = NULL;
  if (bsg_anr_jni_cache->initialized) {
    jint result = (*bsg_anr_jni_cache->jvm)
                      ->AttachCurrentThread(bsg_anr_jni_cache->jvm, &env, NULL);
    if (result != 0) {
      BUGSNAG_LOG("Failed to call JNIEnv->AttachCurrentThread(): %d. ANRs will "
                  "not be reported.",
                  result);
      env = NULL;
    }
  }

  // Always continue with the signal handling mechanism because we still need to
  // forward to the Google handler even if we can't do anything or are disabled.

  for (;;) {
    watchdog_wait_for_trigger();

    // Trigger Google ANR processing (occurs on a different thread).
    bsg_google_anr_call();

    // Do our ANR processing. This will no-op if env is NULL or ANR processing
    // is disabled.
    notify_anr_detected(env);

    // Unblock SIGQUIT again so that handle_sigquit() will run again.
    unblock_sigquit();
  }
}

static void handle_sigquit(__unused int signum, siginfo_t *info,
                           void *user_context) {
  // Re-block SIGQUIT so that the Google handler can trigger.
  // Do it in this handler so that the signal pending flags flip on the next
  // context switch and will be off when the next sigquit_watchdog_thread_main()
  // loop runs.
  block_sigquit();

  // The unwind function will be non-null if the NDK plugin is loaded.
  if (unwind_stack_function != NULL) {
    anr_stacktrace_length =
        unwind_stack_function(anr_stacktrace, info, user_context);
  }

  // Tell sigquit_watchdog_thread_main() to report an ANR.
  trigger_sigquit_watchdog_thread();
}

static void install_signal_handler() {
  if (!bsg_google_anr_init()) {
    BUGSNAG_LOG("Failed to initialize Google ANR caller. ANRs won't be sent to "
                "Google.");
    // We can still report to Bugsnag, so continue.
  }

  if (sem_init(&watchdog_thread_semaphore, 0, 0) == 0) {
    should_wait_for_semaphore = true;
  } else {
    BUGSNAG_LOG("Failed to init semaphore");
    // We can still poll watchdog_thread_triggered, so continue.
  }

  // Start the watchdog thread sigquit_watchdog_thread_main().
  if (pthread_create(&watchdog_thread, NULL, sigquit_watchdog_thread_main,
                     NULL) != 0) {
    BUGSNAG_LOG(
        "Could not create ANR watchdog thread. ANRs won't be sent to Bugsnag.");
    return;
  }

  // Install our signal handler.
  struct sigaction handler;
  sigemptyset(&handler.sa_mask);
  handler.sa_sigaction = handle_sigquit;
  handler.sa_flags = SA_SIGINFO;
  // Note: We do NOT save the old handler since the default SIGQUIT handler MUST
  // NOT be called in an Android environment. See anr_google.c.
  if (sigaction(SIGQUIT, &handler, NULL) != 0) {
    BUGSNAG_LOG(
        "Failed to install SIGQUIT handler: %s. ANRs won't be sent to Bugsnag.",
        strerror(errno));
    return;
  }

  // Unblock SIGQUIT so that our newly installed handler can run.
  unblock_sigquit();
}

bool bsg_handler_install_anr(JNIEnv *env, jobject plugin) {
  pthread_mutex_lock(&bsg_anr_handler_config);

  enabled = true;
  if (!installed && bsg_anr_jni_cache_init(env) && plugin != NULL) {
    anr_plugin = (*env)->NewGlobalRef(env, plugin);
    install_signal_handler();
    installed = true;
  }
  pthread_mutex_unlock(&bsg_anr_handler_config);
  return true;
}

void bsg_handler_uninstall_anr() {
  pthread_mutex_lock(&bsg_anr_handler_config);
  enabled = false;
  pthread_mutex_unlock(&bsg_anr_handler_config);
}

void bsg_set_unwind_function(unwind_func func) { unwind_stack_function = func; }