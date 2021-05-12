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
static sem_t anr_reporting_semaphore;
static volatile bool should_report_anr_flag = false;

static JavaVM *bsg_jvm = NULL;
static jmethodID mthd_notify_anr_detected = NULL;
static jobject obj_plugin = NULL;
static jclass frame_class = NULL;
static jmethodID frame_init = NULL;

static bugsnag_stackframe anr_stacktrace[BUGSNAG_FRAMES_MAX];
static ssize_t anr_stacktrace_length = 0;

static unwind_func unwind_stack_function;

// duplication required for JNI methods that originally came from
// bugsnag-plugin-android-ndk. Until a shared C module is available
// for sharing common code when PLAT-5794 is addressed, this
// duplication is a necessary evil.
static bool check_and_clear_exc(JNIEnv *env) {
  if (env == NULL) {
    return false;
  }
  if ((*env)->ExceptionCheck(env)) {
    (*env)->ExceptionClear(env);
    return true;
  }
  return false;
}

static jclass safe_find_class(JNIEnv *env, const char *clz_name) {
  if (env == NULL) {
    return NULL;
  }
  if (clz_name == NULL) {
    return NULL;
  }
  jclass clz = (*env)->FindClass(env, clz_name);
  check_and_clear_exc(env);
  return clz;
}

static jmethodID safe_get_method_id(JNIEnv *env, jclass clz, const char *name,
                                    const char *sig) {
  if (env == NULL || clz == NULL || name == NULL || sig == NULL) {
    return NULL;
  }
  jmethodID methodId = (*env)->GetMethodID(env, clz, name, sig);
  check_and_clear_exc(env);
  return methodId;
}

static bool configure_anr_jni_impl(JNIEnv *env) {
  // get a global reference to the AnrPlugin class
  // https://developer.android.com/training/articles/perf-jni#faq:-why-didnt-findclass-find-my-class
  if (env == NULL) {
    return false;
  }
  int result = (*env)->GetJavaVM(env, &bsg_jvm);
  if (result != 0) {
    return false;
  }

  jclass clz = safe_find_class(env, "com/bugsnag/android/AnrPlugin");
  if (check_and_clear_exc(env) || clz == NULL) {
    return false;
  }
  mthd_notify_anr_detected =
      safe_get_method_id(env, clz, "notifyAnrDetected", "(Ljava/util/List;)V");
  frame_class = safe_find_class(env, "com/bugsnag/android/NativeStackframe");
  frame_class = (*env)->NewGlobalRef(env, frame_class);
  frame_init = safe_get_method_id(
      env, frame_class, "<init>",
      "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/Number;Ljava/lang/"
      "Long;Ljava/lang/Long;Ljava/lang/Long;)V");
  return true;
}

static void safe_delete_local_ref(JNIEnv *env, jobject obj) {
  if (env != NULL) {
    (*env)->DeleteLocalRef(env, obj);
  }
}

static jobject safe_new_object(JNIEnv *env, jclass clz, jmethodID method, ...) {
  if (env == NULL || clz == NULL) {
    return NULL;
  }
  va_list args;
  va_start(args, method);
  jobject obj = (*env)->NewObjectV(env, clz, method, args);
  va_end(args);
  check_and_clear_exc(env);
  return obj;
}

static jstring safe_new_string_utf(JNIEnv *env, const char *str) {
  if (env == NULL || str == NULL) {
    return NULL;
  }
  jstring jstr = (*env)->NewStringUTF(env, str);
  check_and_clear_exc(env);
  return jstr;
}

static bool configure_anr_jni(JNIEnv *env) {
  bool success = configure_anr_jni_impl(env);
  if (!success) {
    BUGSNAG_LOG("Failed to fetch Java VM. ANR handler not installed.");
  }
  return success;
}

static void notify_anr_detected() {
  if (!enabled) {
    return;
  }

  bool should_detach = false;
  JNIEnv *env;
  int result = (*bsg_jvm)->GetEnv(bsg_jvm, (void **)&env, JNI_VERSION_1_4);
  switch (result) {
  case JNI_OK:
    break;
  case JNI_EDETACHED:
    result = (*bsg_jvm)->AttachCurrentThread(bsg_jvm, &env, NULL);
    if (result != 0) {
      BUGSNAG_LOG("Failed to call JNIEnv->AttachCurrentThread(): %d", result);
      return;
    }
    should_detach = true;
    break;
  default:
    BUGSNAG_LOG("Failed to call JNIEnv->GetEnv(): %d", result);
    return;
  }

  jclass list_class = safe_find_class(env, "java/util/LinkedList");
  jmethodID list_init = safe_get_method_id(env, list_class, "<init>", "()V");
  jmethodID list_add =
      safe_get_method_id(env, list_class, "add", "(Ljava/lang/Object;)Z");
  jclass int_class = safe_find_class(env, "java/lang/Integer");
  jmethodID int_init = safe_get_method_id(env, int_class, "<init>", "(I)V");
  jclass long_class = safe_find_class(env, "java/lang/Long");
  jmethodID long_init = safe_get_method_id(env, long_class, "<init>", "(J)V");

  jobject jlist = safe_new_object(env, list_class, list_init);
  for (ssize_t i = 0; i < anr_stacktrace_length; i++) {
    bugsnag_stackframe *frame = anr_stacktrace + i;
    jobject jmethod = safe_new_string_utf(env, frame->method);
    jobject jfilename = safe_new_string_utf(env, frame->filename);
    jobject jline_number =
        safe_new_object(env, int_class, int_init, (jint)frame->line_number);
    jobject jframe_address = safe_new_object(env, long_class, long_init,
                                             (jlong)frame->frame_address);
    jobject jsymbol_address = safe_new_object(env, long_class, long_init,
                                              (jlong)frame->symbol_address);
    jobject jload_address =
        safe_new_object(env, long_class, long_init, (jlong)frame->load_address);
    jobject jframe = safe_new_object(env, frame_class, frame_init, jmethod,
                                     jfilename, jline_number, jframe_address,
                                     jsymbol_address, jload_address);
    if (jlist != NULL && list_add != NULL && jframe != NULL) {
      (*env)->CallBooleanMethod(env, jlist, list_add, jframe);
      check_and_clear_exc(env);
    }
    safe_delete_local_ref(env, jmethod);
    safe_delete_local_ref(env, jfilename);
    safe_delete_local_ref(env, jline_number);
    safe_delete_local_ref(env, jframe_address);
    safe_delete_local_ref(env, jsymbol_address);
    safe_delete_local_ref(env, jload_address);
    safe_delete_local_ref(env, jframe);
  }

  if (obj_plugin != NULL && mthd_notify_anr_detected != NULL && jlist != NULL) {
    (*env)->CallVoidMethod(env, obj_plugin, mthd_notify_anr_detected, jlist);
    check_and_clear_exc(env);
  }

  if (should_detach) {
    (*bsg_jvm)->DetachCurrentThread(
        bsg_jvm); // detach to restore initial condition
  }
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
  should_report_anr_flag = true;

  if (should_wait_for_semaphore) {
    // Although sem_post() is not officially marked as async-safe, the Android
    // implementation simply does an atomic compare-and-exchange when there is
    // only one thread waiting (which is the case here).
    // https://cs.android.com/android/platform/superproject/+/master:bionic/libc/bionic/semaphore.cpp;l=289?q=sem_post&ss=android
    if (sem_post(&anr_reporting_semaphore) != 0) {
      // The only possible failure from sem_post is EOVERFLOW, which won't
      // happen in this code. But just to be thorough...
      BUGSNAG_LOG("Could not unlock Bugsnag sigquit handler semaphore");
    }
  }
}

static void *sigquit_watchdog_thread_main(__unused void *_) {
  static const useconds_t delay_100ms = 100000;

  while (enabled) {
    // Unblock SIGQUIT so that handle_sigquit() will be called.
    unblock_sigquit();

    // Wait until our SIGQUIT handler is ready for us to start.
    // Use sem_wait() if possible, falling back to polling.
    should_report_anr_flag = false;
    if (!should_wait_for_semaphore || sem_wait(&anr_reporting_semaphore) != 0) {
      while (!should_report_anr_flag) {
        usleep(delay_100ms);
      }
    }

    if (!enabled) {
      // This happens if bsg_handler_uninstall_anr() woke us.
      break;
    }

    // Trigger Google ANR processing (occurs on a different thread).
    bsg_google_anr_call();

    // Do our ANR processing.
    notify_anr_detected();
  }

  return NULL;
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

  if (sem_init(&anr_reporting_semaphore, 0, 0) == 0) {
    should_wait_for_semaphore = true;
  } else {
    BUGSNAG_LOG("Failed to init semaphore");
    // We can still poll should_report_anr_flag, so continue.
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
  if (!installed && configure_anr_jni(env) && plugin != NULL) {
    obj_plugin = (*env)->NewGlobalRef(env, plugin);
    install_signal_handler();
    installed = true;
  }
  pthread_mutex_unlock(&bsg_anr_handler_config);
  return true;
}

void bsg_handler_uninstall_anr() {
  pthread_mutex_lock(&bsg_anr_handler_config);
  enabled = false;
  // Trigger sigquit_watchdog_thread_main() so that it can exit.
  trigger_sigquit_watchdog_thread();
  pthread_mutex_unlock(&bsg_anr_handler_config);
}

void bsg_set_unwind_function(unwind_func func) { unwind_stack_function = func; }
