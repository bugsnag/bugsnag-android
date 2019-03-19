#include "cpp_handler.h"
#include <cxxabi.h>
#include <exception>
#include <pthread.h>
#include <stdexcept>
#include <string>

#include "../utils/crash_info.h"
#include "../utils/serializer.h"
#include "../utils/string.h"
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

void bsg_write_current_exception_message(char *message, size_t length) {
  try {
    throw;
  } catch (std::exception &exc) {
    bsg_strncpy(message, (char *)exc.what(), length);
  } catch (std::exception *exc) {
    bsg_strncpy(message, (char *)exc->what(), length);
  } catch (std::string obj) {
    bsg_strncpy(message, (char *)obj.c_str(), length);
  } catch (char *obj) {
    snprintf(message, length, "%s", obj);
  } catch (char obj) {
    snprintf(message, length, "%c", obj);
  } catch (short obj) {
    snprintf(message, length, "%d", obj);
  } catch (int obj) {
    snprintf(message, length, "%d", obj);
  } catch (long obj) {
    snprintf(message, length, "%ld", obj);
  } catch (long long obj) {
    snprintf(message, length, "%lld", obj);
  } catch (long double obj) {
    snprintf(message, length, "%Lf", obj);
  } catch (double obj) {
    snprintf(message, length, "%f", obj);
  } catch (float obj) {
    snprintf(message, length, "%f", obj);
  } catch (unsigned char obj) {
    snprintf(message, length, "%u", obj);
  } catch (unsigned short obj) {
    snprintf(message, length, "%u", obj);
  } catch (unsigned int obj) {
    snprintf(message, length, "%u", obj);
  } catch (unsigned long obj) {
    snprintf(message, length, "%lu", obj);
  } catch (unsigned long long obj) {
    snprintf(message, length, "%llu", obj);
  } catch (...) {
    // no way to describe what this is
  }
}

void bsg_handle_cpp_terminate() {
  if (bsg_global_env == NULL || bsg_global_env->handling_crash)
    return;

  bsg_global_env->handling_crash = true;
  bsg_populate_report_as(bsg_global_env);
  bsg_global_env->next_report.unhandled_events++;
  bsg_global_env->next_report.exception.frame_count = bsg_unwind_stack(
      bsg_global_env->unwind_style,
      bsg_global_env->next_report.exception.stacktrace, NULL, NULL);

  std::type_info *tinfo = __cxxabiv1::__cxa_current_exception_type();
  if (tinfo != NULL) {
    bsg_strncpy(bsg_global_env->next_report.exception.name,
                (char *)tinfo->name(),
                sizeof(bsg_global_env->next_report.exception.name));
  }
  size_t message_length = sizeof(bsg_global_env->next_report.exception.message);
  char message[message_length];
  bsg_write_current_exception_message(message, message_length);
  bsg_strncpy(bsg_global_env->next_report.exception.message, (char *)message,
              message_length);

  bsg_serialize_report_to_file(bsg_global_env);
  bsg_global_env->crash_handled = true;
  bsg_handler_uninstall_cpp();
  if (bsg_global_terminate_previous != NULL) {
    bsg_global_terminate_previous();
  }
}
