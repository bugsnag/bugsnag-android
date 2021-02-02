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
static sem_t reporter_thread_semaphore;
static volatile bool should_report_anr = false;
static struct sigaction original_sigquit_handler;

static JavaVM *bsg_jvm = NULL;
static jmethodID mthd_notify_anr_detected = NULL;
static jobject obj_plugin = NULL;
static jclass frame_class = NULL;
static jmethodID frame_init = NULL;

static bugsnag_stackframe anr_stacktrace[BUGSNAG_FRAMES_MAX];
static ssize_t anr_stacktrace_length = 0;

unwind_func local_bsg_unwind_stack;

static bool configure_anr_jni(JNIEnv *env) {
  // get a global reference to the AnrPlugin class
  // https://developer.android.com/training/articles/perf-jni#faq:-why-didnt-findclass-find-my-class
  int result = (*env)->GetJavaVM(env, &bsg_jvm);
  if (result != 0) {
    BUGSNAG_LOG("Failed to fetch Java VM. ANR handler not installed.");
    return false;
  }

  jclass clz = (*env)->FindClass(env, "com/bugsnag/android/AnrPlugin");
  mthd_notify_anr_detected =
      (*env)->GetMethodID(env, clz, "notifyAnrDetected", "(Ljava/util/List;)V");
  frame_class = (*env)->FindClass(env, "com/bugsnag/android/NativeStackframe");
  frame_class = (*env)->NewGlobalRef(env, frame_class);
  frame_init = (*env)->GetMethodID(
      env, frame_class, "<init>",
      "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/Number;Ljava/lang/"
      "Long;Ljava/lang/Long;Ljava/lang/Long;)V");
  return true;
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

  jclass list_class = (*env)->FindClass(env, "java/util/LinkedList");
  jmethodID list_init = (*env)->GetMethodID(env, list_class, "<init>", "()V");
  jmethodID list_add =
      (*env)->GetMethodID(env, list_class, "add", "(Ljava/lang/Object;)Z");
  jclass int_class = (*env)->FindClass(env, "java/lang/Integer");
  jmethodID int_init = (*env)->GetMethodID(env, int_class, "<init>", "(I)V");
  jclass long_class = (*env)->FindClass(env, "java/lang/Long");
  jmethodID long_init = (*env)->GetMethodID(env, long_class, "<init>", "(J)V");

  jobject jlist = (*env)->NewObject(env, list_class, list_init);
  for (ssize_t i = 0; i < anr_stacktrace_length; i++) {
    bugsnag_stackframe *frame = anr_stacktrace + i;
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

  if (should_detach) {
    (*bsg_jvm)->DetachCurrentThread(
        bsg_jvm); // detach to restore initial condition
  }
}

static void *sigquit_watchdog_thread_main(void *_) {
  static const useconds_t delay_2sec = 2000000;
  static const useconds_t delay_100ms = 100000;
  static const useconds_t delay_10ms = 10000;

  // Wait until our SIGQUIT handler is ready for us to start.
  // Use sem_wait if possible, falling back to polling.
  if (!should_wait_for_semaphore || sem_wait(&reporter_thread_semaphore) != 0) {
    while (!should_report_anr) {
      usleep(delay_100ms);
    }
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

  // The unwind function will be non-null if the NDK plugin is loaded.
  if (local_bsg_unwind_stack != NULL) {
    anr_stacktrace_length =
        local_bsg_unwind_stack(anr_stacktrace, info, user_context);
  }

  // Instruct our watchdog thread to report the ANR and also call Google
  should_report_anr = true;
  // Although sem_post is not officially marked as async-safe, the Android
  // implementation simply does an atomic compare-and-exchange when there is
  // only one thread waiting (which is the case here).
  // https://cs.android.com/android/platform/superproject/+/master:bionic/libc/bionic/semaphore.cpp;l=289?q=sem_post&ss=android
  if (sem_post(&reporter_thread_semaphore) != 0) {
    // The only possible failure from sem_post is EOVERFLOW, which won't happen
    // in this code. But implementations can change...
    BUGSNAG_LOG("Could not unlock semaphore");
  }
}

static void install_signal_handler() {
  if (!bsg_google_anr_init()) {
    BUGSNAG_LOG("Failed to initialize Google ANR caller. ANRs won't be sent to "
                "Google.");
    // We can still report to Bugsnag, so continue.
  }

  if (sem_init(&reporter_thread_semaphore, 0, 0) == 0) {
    should_wait_for_semaphore = true;
  } else {
    BUGSNAG_LOG("Failed to init semaphore");
    // We can still poll should_report_anr, so continue.
  }

  // Start the watchdog thread
  if (pthread_create(&watchdog_thread, NULL, sigquit_watchdog_thread_main,
                     NULL) != 0) {
    BUGSNAG_LOG(
        "Could not create ANR watchdog thread. ANRs won't be sent to Bugsnag.");
    return;
  }

  // Install our signal handler
  struct sigaction handler;
  sigemptyset(&handler.sa_mask);
  handler.sa_sigaction = handle_sigquit;
  handler.sa_flags = SA_SIGINFO;
  if (sigaction(SIGQUIT, &handler, &original_sigquit_handler) != 0) {
    BUGSNAG_LOG(
        "Failed to install SIGQUIT handler: %s. ANRs won't be sent to Bugsnag.",
        strerror(errno));
    return;
  }

  // Unblock SIGQUIT so that our handler will be called
  sigset_t anr_sigmask;
  sigemptyset(&anr_sigmask);
  sigaddset(&anr_sigmask, SIGQUIT);
  if (pthread_sigmask(SIG_UNBLOCK, &anr_sigmask, NULL) != 0) {
    BUGSNAG_LOG("Could not unblock SIGQUIT. ANRs won't be sent to Bugsnag.");
  }
}

bool bsg_handler_install_anr(JNIEnv *env, jobject plugin) {
  pthread_mutex_lock(&bsg_anr_handler_config);

  if (!installed && configure_anr_jni(env)) {
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
