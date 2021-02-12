#include "anr_handler.h"
#include <android/log.h>
#include <errno.h>
#include <jni.h>
#include <pthread.h>
#include <signal.h>
#include <string.h>
#include <unistd.h>

#include "anr_google.h"

// Lock for changing the handler configuration
static pthread_mutex_t bsg_anr_handler_config = PTHREAD_MUTEX_INITIALIZER;

// A proxy for install/uninstall state, to avoid needing to unset the handler
// on the sigquit-watching thread
static bool enabled = false;
static bool installed = false;

static pthread_t watchdog_thread;
static volatile bool should_report_anr = false;
static struct sigaction original_sigquit_handler;

static JavaVM *bsg_jvm = NULL;
static jmethodID mthd_notify_anr_detected = NULL;
static jobject obj_plugin = NULL;

// duplication required for this method that originally came from
// bugsnag-plugin-android-ndk. Until a shared C module is available
// for sharing common code when PLAT-5794 is addressed, this
// duplication is a necessary evil.
bool bsg_check_and_clear_exc(JNIEnv *env) {
  if (env == NULL) {
    return false;
  }
  if ((*env)->ExceptionCheck(env)) {
    (*env)->ExceptionClear(env);
    return true;
  }
  return false;
}

bool configure_anr_jni_impl(JNIEnv *env) {// get a global reference to the AnrPlugin class
// https://developer.android.com/training/articles/perf-jni#faq:-why-didnt-findclass-find-my-class
  if (env == NULL) {
    return false;
  }
  int result = (*env)->GetJavaVM(env, &bsg_jvm);
  if (result != 0) {
    return false;
  }

  jclass clz = (*env)->FindClass(env, "com/bugsnag/android/AnrPlugin");
  if (bsg_check_and_clear_exc(env) || clz == NULL) {
    return false;
  }
  mthd_notify_anr_detected =
      (*env)->GetMethodID(env, clz, "notifyAnrDetected", "()V");
  if (bsg_check_and_clear_exc(env) || mthd_notify_anr_detected == NULL) {
    return false;
  }
  return true;
}

static bool configure_anr_jni(JNIEnv *env) {
  bool success = configure_anr_jni_impl(env);
  if (!success) {
    BUGSNAG_LOG("Failed to fetch Java VM. ANR handler not installed.");
  }
  return success;
}

void notify_anr_detected_impl(JNIEnv *env) {
  if (env != NULL && obj_plugin != NULL && mthd_notify_anr_detected != NULL) {
    (*env)->CallVoidMethod(env, obj_plugin, mthd_notify_anr_detected);
    bsg_check_and_clear_exc(env);
  }
}

static void notify_anr_detected() {
  if (enabled) {
    JNIEnv *env;
    int result = (*bsg_jvm)->GetEnv(bsg_jvm, (void **)&env, JNI_VERSION_1_4);

    if (result == JNI_OK) { // already attached
      notify_anr_detected_impl(env);
    } else if (result == JNI_EDETACHED) { // attach before calling JNI
      if ((*bsg_jvm)->AttachCurrentThread(bsg_jvm, &env, NULL) == 0) {
        notify_anr_detected_impl(env);
        (*bsg_jvm)->DetachCurrentThread(bsg_jvm); // detach to restore initial condition
      }
    } // All other results are error codes
  }
}

static void *sigquit_watchdog_thread_main(void *_) {
  static const useconds_t delay_2sec = 2000000;
  static const useconds_t delay_100ms = 100000;
  static const useconds_t delay_10ms = 10000;

  // Wait until our SIGQUIT handler is ready for us to start
  while (!should_report_anr) {
    usleep(delay_100ms);
  }

  // Force at least one task switch after being triggered, ensuring that the
  // signal masks are properly settled before triggering the Google handler.
  usleep(delay_10ms);

  // Do our ANR processing
  notify_anr_detected();

  // Trigger Google ANR processing
  bsg_google_anr_call();

  // Give a little time for the Google handler to dump state, then exit this
  // thread.
  usleep(delay_2sec);
  return NULL;
}

static void handle_sigquit(int signum, siginfo_t *info, void *user_context) {
  // Re-block SIGQUIT so that the Google handler can trigger
  sigset_t sigmask;
  sigemptyset(&sigmask);
  sigaddset(&sigmask, SIGQUIT);
  pthread_sigmask(SIG_BLOCK, &sigmask, NULL);
  sigaction(SIGQUIT, &original_sigquit_handler, NULL);

  // Instruct our watchdog thread to report the ANR and also call Google
  should_report_anr = true;
}

static void install_signal_handler() {
  if (!bsg_google_anr_init()) {
    BUGSNAG_LOG("Failed to initialize Google ANR caller. ANRs won't be sent to "
                "Google.");
  }

  // Start the watchdog thread
  pthread_create(&watchdog_thread, NULL, sigquit_watchdog_thread_main, NULL);

  // Install our signal handler
  struct sigaction handler;
  sigemptyset(&handler.sa_mask);
  handler.sa_sigaction = handle_sigquit;
  handler.sa_flags = SA_SIGINFO;
  int success = sigaction(SIGQUIT, &handler, &original_sigquit_handler);
  if (success != 0) {
    BUGSNAG_LOG("Failed to install SIGQUIT handler: %s", strerror(errno));
    return;
  }

  // Unblock SIGQUIT so that our handler will be called
  sigset_t anr_sigmask;
  sigemptyset(&anr_sigmask);
  sigaddset(&anr_sigmask, SIGQUIT);
  pthread_sigmask(SIG_UNBLOCK, &anr_sigmask, NULL);
}

bool bsg_handler_install_anr(JNIEnv *env, jobject plugin,
                             jboolean callPreviousSigquitHandler) {
  pthread_mutex_lock(&bsg_anr_handler_config);

  if (!installed && configure_anr_jni(env) && plugin != NULL) {
    obj_plugin = (*env)->NewGlobalRef(env, plugin);
    install_signal_handler();
    installed = true;
  }
  enabled = true;
  pthread_mutex_unlock(&bsg_anr_handler_config);
  return true;
}

void bsg_handler_uninstall_anr() {
  pthread_mutex_lock(&bsg_anr_handler_config);
  enabled = false;
  pthread_mutex_unlock(&bsg_anr_handler_config);
}
