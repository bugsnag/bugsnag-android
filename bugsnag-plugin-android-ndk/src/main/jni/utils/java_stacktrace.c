#include "java_stacktrace.h"
#include "jni_cache.h"
#include "safejni.h"
#include "string.h"

void bsg_copy_java_stacktrace(JNIEnv *env, jobject stack_trace,
                              bsg_error *error) {
  if (stack_trace == NULL) {
    error->frame_count = 0;
    return;
  }

  jsize stack_length = bsg_safe_get_array_length(env, stack_trace);
  int frame_count =
      stack_length < BUGSNAG_FRAMES_MAX ? stack_length : BUGSNAG_FRAMES_MAX;

  for (int i = 0; i < frame_count; i++) {
    (*env)->PushLocalFrame(env, 5);
    jobject stack_element =
        bsg_safe_get_object_array_element(env, stack_trace, i);
    if (stack_element == NULL) {
      (*env)->PopLocalFrame(env, NULL);
      continue;
    }

    bugsnag_stackframe *frame = &error->stacktrace[i];

    // Get file name
    jstring file_name = (jstring)bsg_safe_call_object_method(
        env, stack_element, bsg_jni_cache->StackTraceElement_getFileName);
    if (file_name != NULL) {
      const char *class_chars = bsg_safe_get_string_utf_chars(env, file_name);
      if (class_chars != NULL) {
        bsg_strncpy(frame->filename, class_chars, sizeof(frame->filename));
        bsg_safe_release_string_utf_chars(env, file_name, class_chars);
      }
    }

    // Get class name and set copy it into frame->method
    jstring class_name = (jstring)bsg_safe_call_object_method(
        env, stack_element, bsg_jni_cache->StackTraceElement_getClassName);
    size_t class_length = 0;
    if (file_name != NULL) {
      const char *class_chars = bsg_safe_get_string_utf_chars(env, class_name);
      if (class_chars != NULL && *class_chars != 0) {
        class_length =
            bsg_strncpy(frame->method, class_chars, sizeof(frame->method));
        bsg_safe_release_string_utf_chars(env, class_name, class_chars);
      }
    }

    // Get method name & concat to the frame->method
    jstring method_name = (jstring)bsg_safe_call_object_method(
        env, stack_element, bsg_jni_cache->StackTraceElement_getMethodName);
    if (method_name != NULL) {
      const char *method_chars =
          bsg_safe_get_string_utf_chars(env, method_name);
      if (method_chars != NULL) {
        if (class_length + 1 < sizeof(frame->method)) {
          char *method = &frame->method[class_length];
          *method = '.';
          method++;

          size_t remaining = sizeof(frame->method) - class_length - 1;
          bsg_strncpy(method, method_chars, remaining);
        }

        bsg_safe_release_string_utf_chars(env, method_name, method_chars);
      }
    }

    // Get line number
    frame->line_number = bsg_safe_call_int_method(
        env, stack_element, bsg_jni_cache->StackTraceElement_getLineNumber);

    (*env)->PopLocalFrame(env, NULL);
  }

  error->frame_count = frame_count;
}
