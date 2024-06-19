/**
 * async-safe functions for writing an event to disk
 */
#pragma once
#include "../../bugsnag_ndk.h"

bool bsg_event_write(bsg_environment *env) __asyncsafe;

bool bsg_write_event_file(bsg_environment *env,
                          const char *filename) __asyncsafe;

bool bsg_lastrun_write(bsg_environment *env) __asyncsafe;
