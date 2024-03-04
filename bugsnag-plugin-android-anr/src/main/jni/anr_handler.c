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

#define JNI_VERSION JNI_VERSION_1_6

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

static jmethodID mthd_notify_anr_detected = NULL;
static jobject obj_plugin = NULL;
static jclass frame_class = NULL;
static jobject error_type = NULL;
static jmethodID frame_init = NULL;

static bugsnag_stackframe anr_stacktrace[BUGSNAG_FRAMES_MAX];
static ssize_t anr_stacktrace_length = 0;

static unwind_func unwind_stack_function;

// duplication required for JNI methods that originally came from
// bugsnag-plugin-android-ndk. Until a shared C module is available
// for sharing common code when PLAT-5794 is addressed, this
// duplication is a necessary evil.

static JavaVM *jvm = NULL;
static pthread_key_t jni_cleanup_key;

static void detach_env(void *env) {
  if (jvm != NULL && env != NULL) {
    (*jvm)->DetachCurrentThread(jvm);
  }
}

static JNIEnv *get_env() {
  if (jvm == NULL) {
    return NULL;
  }

  JNIEnv *env = NULL;
  switch ((*jvm)->GetEnv(jvm, (void **)&env, JNI_VERSION)) {
  case JNI_OK:
    return env;
  case JNI_EDETACHED:
    if ((*jvm)->AttachCurrentThread(jvm, &env, NULL) != JNI_OK) {
      BUGSNAG_LOG("Could not attach thread to JVM");
      return NULL;
    }
    if (env == NULL) {
      BUGSNAG_LOG("AttachCurrentThread filled a NULL JNIEnv");
      return NULL;
    }

    // attach a destructor to detach the env before the thread terminates
    pthread_setspecific(jni_cleanup_key, env);

    return env;
  default:
    BUGSNAG_LOG("Could not get JNIEnv");
    return NULL;
  }
}

static bool check_and_clear_exc(JNIEnv *env) {
  if (env == NULL) {
    return false;
  }
  if ((*env)->ExceptionCheck(env)) {
    BUGSNAG_LOG("BUG: JNI Native->Java call threw an exception:");

    // Print a trace to stderr so that we can debug it
    (*env)->ExceptionDescribe(env);

    // Trigger more accurate dalvik trace (this will also crash the app).

    // Code review check: THIS MUST BE COMMENTED OUT IN CHECKED IN CODE!
    //(*env)->FindClass(env, NULL);

    // Clear the exception so that we don't crash.
    (*env)->ExceptionClear(env);
    return true;
  }
  return false;
}

static jclass safe_find_class(JNIEnv *env, const char *clz_name) {
  if (env == NULL || clz_name == NULL) {
    return NULL;
  }
  jclass clz = (*env)->FindClass(env, clz_name);
  if (check_and_clear_exc(env)) {
    return NULL;
  }
  return clz;
}

static jmethodID safe_get_method_id(JNIEnv *env, jclass clz, const char *name,
                                    const char *sig) {
  if (env == NULL || clz == NULL || name == NULL || sig == NULL) {
    return NULL;
  }
  jmethodID methodId = (*env)->GetMethodID(env, clz, name, sig);
  if (check_and_clear_exc(env)) {
    return NULL;
  }
  return methodId;
}

static jfieldID safe_get_static_field_id(JNIEnv *env, jclass clz,
                                         const char *name, const char *sig) {
  if (env == NULL || clz == NULL || name == NULL || sig == NULL) {
    return NULL;
  }
  jfieldID fid = (*env)->GetStaticFieldID(env, clz, name, sig);
  if (check_and_clear_exc(env)) {
    return NULL;
  }
  return fid;
}

static jobject safe_get_static_object_field(JNIEnv *env, jclass clz,
                                            jfieldID field) {
  if (env == NULL || clz == NULL || field == NULL) {
    return NULL;
  }
  jobject obj = (*env)->GetStaticObjectField(env, clz, field);
  if (check_and_clear_exc(env)) {
    return NULL;
  }
  return obj;
}

static jobject safe_new_object(JNIEnv *env, jclass clz, jmethodID method, ...) {
  if (env == NULL || clz == NULL) {
    return NULL;
  }
  va_list args;
  va_start(args, method);
  jobject obj = (*env)->NewObjectV(env, clz, method, args);
  va_end(args);
  if (check_and_clear_exc(env)) {
    return NULL;
  }
  return obj;
}

static jstring safe_new_string_utf(JNIEnv *env, const char *str) {
  if (env == NULL || str == NULL) {
    return NULL;
  }
  jstring jstr = (*env)->NewStringUTF(env, str);
  if (check_and_clear_exc(env)) {
    return NULL;
  }
  return jstr;
}

static void safe_delete_local_ref(JNIEnv *env, jobject obj) {
  if (env != NULL && obj != NULL) {
    (*env)->DeleteLocalRef(env, obj);
  }
}

static jint safe_push_local_frame(JNIEnv *env, const int size) {
  if (env != NULL) {
    return (*env)->PushLocalFrame(env, size);
  }
  return 0;
}

static void safe_pop_local_frame(JNIEnv *env) {
  if (env != NULL) {
    (*env)->PopLocalFrame(env, NULL);
  }
}

// End of duplication

static bool configure_anr_jni_impl(JNIEnv *env) {
  if (env == NULL) {
    return false;
  }

  jclass clz = safe_find_class(env, "com/bugsnag/android/AnrPlugin");
  if (clz == NULL) {
    return false;
  }
  mthd_notify_anr_detected =
      safe_get_method_id(env, clz, "notifyAnrDetected", "(Ljava/util/List;)V");
  if (mthd_notify_anr_detected == NULL) {
    return false;
  }

  // find ErrorType class
  jclass error_type_class =
      safe_find_class(env, "com/bugsnag/android/ErrorType");
  if (error_type_class == NULL) {
    return false;
  }
  jfieldID error_type_field = safe_get_static_field_id(
      env, error_type_class, "C", "Lcom/bugsnag/android/ErrorType;");
  if (error_type_field == NULL) {
    return false;
  }
  error_type =
      safe_get_static_object_field(env, error_type_class, error_type_field);
  if (error_type == NULL) {
    return false;
  }
  error_type = (*env)->NewGlobalRef(env, error_type);

  // find NativeStackFrame class
  frame_class = safe_find_class(env, "com/bugsnag/android/NativeStackframe");
  if (frame_class == NULL) {
    return false;
  }
  frame_class = (*env)->NewGlobalRef(env, frame_class);

  // find NativeStackframe ctor
  frame_init = safe_get_method_id(
      env, frame_class, "<init>",
      "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/Number;Ljava/lang/"
      "Long;Ljava/lang/Long;Ljava/lang/Long;Ljava/lang/Boolean;Lcom/bugsnag/"
      "android/ErrorType;Ljava/lang/String;)V");
  if (frame_init == NULL) {
    return false;
  }

  // Initialize jvm last so that it's guaranteed NULL if any part of the init
  // goes wrong.
  if ((*env)->GetJavaVM(env, &jvm) != JNI_OK) {
    return false;
  }

  pthread_key_create(&jni_cleanup_key, detach_env);
  return true;
}

static bool configure_anr_jni(JNIEnv *env) {
  bool success = configure_anr_jni_impl(env);
  if (!success) {
    BUGSNAG_LOG("Failed to fetch Java VM. ANR handler not installed.");
  }
  return success;
}

static void notify_anr_detected() {
  if (!enabled || obj_plugin == NULL) {
    return;
  }

  JNIEnv *env = get_env();
  if (env == NULL) {
    return;
  }

  jclass list_class = safe_find_class(env, "java/util/LinkedList");
  if (list_class == NULL) {
    return;
  }
  jmethodID list_init = safe_get_method_id(env, list_class, "<init>", "()V");
  if (list_init == NULL) {
    return;
  }
  jmethodID list_add =
      safe_get_method_id(env, list_class, "add", "(Ljava/lang/Object;)Z");
  if (list_add == NULL) {
    return;
  }
  jclass int_class = safe_find_class(env, "java/lang/Integer");
  if (int_class == NULL) {
    return;
  }
  jmethodID int_init = safe_get_method_id(env, int_class, "<init>", "(I)V");
  if (int_init == NULL) {
    return;
  }
  jclass long_class = safe_find_class(env, "java/lang/Long");
  if (long_class == NULL) {
    return;
  }
  jmethodID long_init = safe_get_method_id(env, long_class, "<init>", "(J)V");
  if (long_init == NULL) {
    return;
  }

  jobject jlist = safe_new_object(env, list_class, list_init);
  if (jlist == NULL) {
    return;
  }
  for (ssize_t i = 0; i < anr_stacktrace_length; i++) {
    if (safe_push_local_frame(env, 7) != 0) {
      // there is a pending error, so we exit rather than trying to continue
      break;
    }
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
    jobject jframe = safe_new_object(
        env, frame_class, frame_init, jmethod, jfilename, jline_number,
        jframe_address, jsymbol_address, jload_address, NULL, error_type, NULL);
    if (jframe != NULL) {
      (*env)->CallBooleanMethod(env, jlist, list_add, jframe);
      check_and_clear_exc(env);
    }
    safe_pop_local_frame(env);
  }

  (*env)->CallVoidMethod(env, obj_plugin, mthd_notify_anr_detected, jlist);
  check_and_clear_exc(env);
  safe_delete_local_ref(env, jlist);
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

  for (;;) {
    watchdog_wait_for_trigger();

    // Trigger Google ANR processing (occurs on a different thread).
    bsg_google_anr_call();

    // Trigger our ANR processing on our JNI worker thread (if enabled).
    notify_anr_detected();

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
  pthread_mutex_unlock(&bsg_anr_handler_config);
}

void bsg_set_unwind_function(unwind_func func) { unwind_stack_function = func; }