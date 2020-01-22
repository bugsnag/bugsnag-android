#include "anr_handler.h"
#include <errno.h>
#include <pthread.h>
#include <signal.h>
#include <string.h>
#include <android/log.h>
#include <jni.h>

#include "utils/string.h"

static pthread_t bsg_watchdog_thread;
static sigset_t bsg_anr_sigmask;

// Lock for changing the handler configuration
static pthread_mutex_t bsg_anr_handler_config = PTHREAD_MUTEX_INITIALIZER;
// A proxy for install/uninstall state, to avoid needing to unset the handler
// on the sigquit-watching thread
static bool enabled = false;
static bool installed = false;
static bool invokePrevHandler = true;

static JavaVM *bsg_jvm = NULL;
static jmethodID mthd_notify_anr_detected = NULL;
static jobject obj_plugin = NULL;

/* The previous SIGQUIT handler */
struct sigaction bsg_sigquit_sigaction_previous;


void bsg_notify_anr_detected(JNIEnv *env) {
  (*env)->CallVoidMethod(env, obj_plugin, mthd_notify_anr_detected);
}

bool bsg_configure_anr_jni(JNIEnv *env) {
  // get a global reference to the AnrPlugin class
  // https://developer.android.com/training/articles/perf-jni#faq:-why-didnt-findclass-find-my-class
  int result = (*env)->GetJavaVM(env, &bsg_jvm);
  if (result != 0) {
    // Failed to fetch VM, cannot continue
    return false;
  }

  jclass clz = (*env)->FindClass(env, "com/bugsnag/android/AnrPlugin");
  mthd_notify_anr_detected = (*env)->GetMethodID(env, clz, "notifyAnrDetected", "()V");
  return true;
}

void bsg_handle_sigquit(int signum, siginfo_t *info, void *user_context) {
  if (enabled) {
    // invoke a JNI call from the SIGQUIT handler
    JNIEnv *env;
    int result = (*bsg_jvm)->GetEnv(bsg_jvm, (void **)&env, JNI_VERSION_1_4);

    if (result == JNI_OK) { // already attached
      bsg_notify_anr_detected(env);
    } else if (result == JNI_EDETACHED) { // attach before calling JNI
      if ((*bsg_jvm)->AttachCurrentThread(bsg_jvm, &env, NULL) == 0) {
        bsg_notify_anr_detected(env);
        (*bsg_jvm)->DetachCurrentThread(bsg_jvm); // detach to restore initial condition
      }
    } // All other results are error codes
  }

  // Invoke previous handler, if a custom handler has been set
  // This can be conditionally switched off on certain platforms (e.g. Unity) where
  // this behaviour is not desirable due to Unity's implementation failing with a SIGSEGV
  if (invokePrevHandler) {
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
}

/**
 * Configure ANR detection using a signal handler listening for SIGQUIT
 */
void *bsg_monitor_anrs(void *_arg) {
  struct sigaction handler;
  sigemptyset(&handler.sa_mask);
  handler.sa_sigaction = bsg_handle_sigquit;

  // remove SA_ONSTACK flag as we don't require information about the SIGQUIT signal.
  // specifying this flag results in a crash when performing a JNI call. For further context
  // see https://issuetracker.google.com/issues/37035211
  handler.sa_flags = SA_SIGINFO;
  int success = sigaction(SIGQUIT, &handler, &bsg_sigquit_sigaction_previous);
  if (success != 0) {
    BUGSNAG_LOG("Failed to install SIGQUIT handler: %s", strerror(errno));
  }

  return NULL;
}

bool bsg_handler_install_anr(JNIEnv *env, jobject plugin, jboolean callPreviousSigquitHandler) {
  pthread_mutex_lock(&bsg_anr_handler_config);
  invokePrevHandler = callPreviousSigquitHandler;

  if (!installed && bsg_configure_anr_jni(env)) {
    obj_plugin = (*env)->NewGlobalRef(env, plugin);
    sigemptyset(&bsg_anr_sigmask);
    sigaddset(&bsg_anr_sigmask, SIGQUIT);

    int mask_status = pthread_sigmask(SIG_BLOCK, &bsg_anr_sigmask, NULL);
    if (mask_status != 0) {
      BUGSNAG_LOG("Failed to mask SIGQUIT: %s", strerror(mask_status));
    } else {
      pthread_create(&bsg_watchdog_thread, NULL, bsg_monitor_anrs, NULL);
      // unblock the current thread
      pthread_sigmask(SIG_UNBLOCK, &bsg_anr_sigmask, NULL);
    }
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
