#pragma once
#include <android/log.h>

#ifndef BUGSNAG_LOG
#define BUGSNAG_LOG(fmt, ...)                                                  \
  __android_log_print(ANDROID_LOG_WARN, "BugsnagNDK", fmt, ##__VA_ARGS__)
#endif
