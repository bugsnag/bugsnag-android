#include "cpp_handler.h"
#include <cxxabi.h>
#include <exception>
#include <pthread.h>
#include <stdexcept>
#include <string>
#include <unistd.h>

#include "../utils/crash_info.h"
#include "../utils/serializer.h"
#include "../utils/string.h"
#include "../utils/threads.h"
/**
 * Previously installed termination handler
 */
std::terminate_handler bsg_global_terminate_previous;
/**
 * Global shared context for Bugsnag reports
 */
static bsg_environment *bsg_global_env;

/**
 * C++ exception handler
 */
void bsg_handle_cpp_terminate();

bool bsg_handler_install_cpp(bsg_environment *env) {
  if (bsg_global_env != NULL) {
    return true; // already installed
  }
  static pthread_mutex_t bsg_cpp_handler_config = PTHREAD_MUTEX_INITIALIZER;
  pthread_mutex_lock(&bsg_cpp_handler_config);
  bsg_global_terminate_previous = std::set_terminate(bsg_handle_cpp_terminate);
  bsg_global_env = env;

  pthread_mutex_unlock(&bsg_cpp_handler_config);
  return true;
}

void bsg_handler_uninstall_cpp() {
  if (bsg_global_env == NULL)
    return;
  std::set_terminate(bsg_global_terminate_previous);
  bsg_global_env = NULL;
}

void bsg_handle_cpp_terminate() {
  if (bsg_global_env == NULL || bsg_global_env->handling_crash)
    return;

  if (!bsg_begin_handling_crash()) {
    return;
  }

  bsg_populate_event_as(bsg_global_env);
  bsg_global_env->next_event.unhandled = true;
  bsg_global_env->next_event.error.frame_count = bsg_unwind_crash_stack(
      bsg_global_env->next_event.error.stacktrace, NULL, NULL);

  if (bsg_global_env->send_threads != SEND_THREADS_NEVER) {
    bsg_global_env->next_event.thread_count = bsg_capture_thread_states(
        gettid(), bsg_global_env->next_event.threads, BUGSNAG_THREADS_MAX);
  } else {
    bsg_global_env->next_event.thread_count = 0;
  }

  std::type_info *tinfo = __cxxabiv1::__cxa_current_exception_type();
  if (tinfo != NULL) {
    bsg_strncpy(bsg_global_env->next_event.error.errorClass,
                (char *)tinfo->name(),
                sizeof(bsg_global_env->next_event.error.errorClass));
  }

  if (bsg_run_on_error()) {
    bsg_increment_unhandled_count(&bsg_global_env->next_event);
    bsg_serialize_event_to_file(bsg_global_env);
    bsg_serialize_last_run_info_to_file(bsg_global_env);
  }

  bsg_finish_handling_crash();
  bsg_handler_uninstall_cpp();
  if (bsg_global_terminate_previous != NULL) {
    bsg_global_terminate_previous();
  }
}
