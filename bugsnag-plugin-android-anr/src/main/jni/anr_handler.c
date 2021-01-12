#include "anr_handler.h"
#include <android/log.h>
#include <errno.h>
#include <jni.h>
#include <pthread.h>
#include <signal.h>
#include <string.h>
#include <unistd.h>

#include "utils/string.h"
#include "unwind_func.h"

static pthread_t bsg_watchdog_thread;
static pthread_t bsg_jvm_notifier_thread;
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
static jclass frame_class;
static jmethodID frame_init;

static volatile bool ready_to_report_anr = false;
static bugsnag_stackframe anr_stacktrace[BUGSNAG_FRAMES_MAX];
static ssize_t anr_stacktrace_length;

unwind_func local_bsg_unwind_stack;
void callNotifyAnrDetected(JNIEnv *env, bugsnag_stackframe* stackTrace, ssize_t length);

/* The previous SIGQUIT handler */
struct sigaction bsg_sigquit_sigaction_previous;

bool bsg_configure_anr_jni(JNIEnv *env) {
  // get a global reference to the AnrPlugin class
  // https://developer.android.com/training/articles/perf-jni#faq:-why-didnt-findclass-find-my-class
  int result = (*env)->GetJavaVM(env, &bsg_jvm);
  if (result != 0) {
    // Failed to fetch VM, cannot continue
    return false;
  }

  jclass clz = (*env)->FindClass(env, "com/bugsnag/android/AnrPlugin");
  mthd_notify_anr_detected =
      (*env)->GetMethodID(env, clz, "notifyAnrDetected", "(Ljava/util/List;)V");
  return true;
}

void callNotifyAnrDetected(JNIEnv *env, bugsnag_stackframe* stacktrace, ssize_t length) {
  jclass list_class = (*env)->FindClass(env, "java/util/LinkedList");
  jmethodID list_init = (*env)->GetMethodID(env, list_class, "<init>", "()V");
  jmethodID list_add = (*env)->GetMethodID(env, list_class, "add", "(Ljava/lang/Object;)Z");
  jclass int_class = (*env)->FindClass(env, "java/lang/Integer");
  jmethodID int_init = (*env)->GetMethodID(env, int_class, "<init>", "(I)V");
  jclass long_class = (*env)->FindClass(env, "java/lang/Long");
  jmethodID long_init = (*env)->GetMethodID(env, long_class, "<init>", "(J)V");

  jobject jlist = (*env)->NewObject(env, list_class, list_init);
  for (ssize_t i = 0; i < length; i++) {
    bugsnag_stackframe *frame = stacktrace + i;
    jobject jmethod = (*env)->NewStringUTF(env, frame->method);
    jobject jfilename = (*env)->NewStringUTF(env, frame->filename);
    jobject jline_number =
            (*env)->NewObject(env, int_class, int_init, (jint)frame->line_number);
    jobject jframe_address = (*env)->NewObject(env, long_class, long_init,
                                               (jlong)frame->frame_address);
    jobject jsymbol_address = (*env)->NewObject(env, long_class, long_init,
                                                (jlong)frame->symbol_address);
    jobject jload_address = (*env)->NewObject(env, long_class, long_init,
                                              (jlong)frame->load_address);
    jobject jframe = (*env)->NewObject(env, frame_class, frame_init, jmethod,
                                       jfilename, jline_number, jframe_address,
                                       jsymbol_address, jload_address);
    (*env)->CallBooleanMethod(env, jlist, list_add, jframe);
    (*env)->DeleteLocalRef(env, jmethod);
    (*env)->DeleteLocalRef(env, jfilename);
    (*env)->DeleteLocalRef(env, jline_number);
    (*env)->DeleteLocalRef(env, jframe_address);
    (*env)->DeleteLocalRef(env, jsymbol_address);
    (*env)->DeleteLocalRef(env, jload_address);
    (*env)->DeleteLocalRef(env, jframe);
  }

  (*env)->CallVoidMethod(env, obj_plugin, mthd_notify_anr_detected, jlist);
}


static void *wait_and_notify_jvm_anr(void *_arg) {
  while(!ready_to_report_anr) {
    usleep(100000);
  }
  usleep(100000);

  if (enabled) {
    JNIEnv *env;
    switch ((*bsg_jvm)->GetEnv(bsg_jvm, (void **)&env, JNI_VERSION_1_4)) {
      case JNI_OK:
        callNotifyAnrDetected(env, anr_stacktrace, anr_stacktrace_length);
      case JNI_EDETACHED:
        if ((*bsg_jvm)->AttachCurrentThread(bsg_jvm, &env, NULL) == 0) {
          callNotifyAnrDetected(env, anr_stacktrace, anr_stacktrace_length);
          (*bsg_jvm)->DetachCurrentThread(bsg_jvm); // detach to restore initial condition
        }
    }
  }

  return NULL;
}

void bsg_handle_sigquit(int signum, siginfo_t *info, void *user_context) {
  // The unwind function will be non-null if the NDK plugin is loaded.
  if (local_bsg_unwind_stack != NULL) {
    anr_stacktrace_length = local_bsg_unwind_stack(anr_stacktrace, info, user_context);
  }
  ready_to_report_anr = true;

  // Leave time for the JVM call to finish
  // TODO: This must be removed in the later ANR revamp
  sleep(2);

  // Invoke previous handler, if a custom handler has been set
  // This can be conditionally switched off on certain platforms (e.g. Unity)
  // where this behaviour is not desirable due to Unity's implementation failing
  // with a SIGSEGV
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

  // remove SA_ONSTACK flag as we don't require information about the SIGQUIT
  // signal. specifying this flag results in a crash when performing a JNI call.
  // For further context see https://issuetracker.google.com/issues/37035211
  handler.sa_flags = SA_SIGINFO;
  int success = sigaction(SIGQUIT, &handler, &bsg_sigquit_sigaction_previous);
  if (success != 0) {
    BUGSNAG_LOG("Failed to install SIGQUIT handler: %s", strerror(errno));
  }

  return NULL;
}

bool bsg_handler_install_anr(JNIEnv *env, jobject plugin,
                             jboolean callPreviousSigquitHandler) {
  pthread_mutex_lock(&bsg_anr_handler_config);
  invokePrevHandler = callPreviousSigquitHandler;

  if (!installed && bsg_configure_anr_jni(env)) {
    obj_plugin = (*env)->NewGlobalRef(env, plugin);
    frame_class = (*env)->FindClass(env, "com/bugsnag/android/NativeStackframe");
    frame_class = (*env)->NewGlobalRef(env, frame_class);
    frame_init = (*env)->GetMethodID(
            env, frame_class, "<init>",
            "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/Number;Ljava/lang/"
            "Long;Ljava/lang/Long;Ljava/lang/Long;)V");

    sigemptyset(&bsg_anr_sigmask);
    sigaddset(&bsg_anr_sigmask, SIGQUIT);

    int mask_status = pthread_sigmask(SIG_BLOCK, &bsg_anr_sigmask, NULL);
    if (mask_status != 0) {
      BUGSNAG_LOG("Failed to mask SIGQUIT: %s", strerror(mask_status));
    } else {
      pthread_create(&bsg_jvm_notifier_thread, NULL, wait_and_notify_jvm_anr, NULL);
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
