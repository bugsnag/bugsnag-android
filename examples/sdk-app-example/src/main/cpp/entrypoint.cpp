#include <bugsnag.h>
#include <jni.h>

extern "C" {

static void __attribute__((used)) somefakefunc(void) {}

int crash_write_read_only() {
  // Write to a read-only page
  volatile char *ptr = (char *)somefakefunc;
  *ptr = 0;

  return 5;
}

bool my_on_error_b(void *event) {
  bugsnag_event_set_user(event, "999999", "ndk override", "j@ex.co");
  bugsnag_event_add_metadata_string(event, "Native", "field", "value");
  bugsnag_event_add_metadata_bool(event, "Native", "field", true);
  return true;
}

JNIEXPORT void JNICALL
Java_com_bugsnag_android_example_ExampleApplication_performNativeBugsnagSetup(
    JNIEnv *env, jobject instance) {
  bugsnag_add_on_error(&my_on_error_b);
}

JNIEXPORT void JNICALL Java_com_bugsnag_android_example_ExampleActivity_doCrash(
    JNIEnv *env, jobject instance) {
  crash_write_read_only();
}

JNIEXPORT void JNICALL
Java_com_bugsnag_android_example_ExampleActivity_notifyFromCXX(
    JNIEnv *env, jobject instance) {
  // Set the current user
  bugsnag_set_user_env(env, "124323", "joe mills", "j@ex.co");
  // Leave a breadcrumb
  bugsnag_leave_breadcrumb_env(env, "Critical failure", BSG_CRUMB_LOG);
  // Send an error report
  bugsnag_notify_env(env, "Oh no", "The mill!", BSG_SEVERITY_INFO);
}
}
