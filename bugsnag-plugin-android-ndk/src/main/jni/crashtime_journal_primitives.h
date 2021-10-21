#ifndef BUGSNAG_CTJOURNAL_H
#define BUGSNAG_CTJOURNAL_H

#include "event.h"
#include <stdbool.h>

/**
 * Initialise the crash-time journaling system. This must be called as early as
 * possible so that all structures will be ready for a C++ or signal-based crash
 * event.
 *
 * @param journal_path /path/to/myjournal.journal.crashtime
 * @return true if initialisation was successful.
 */
bool bsg_ctj_init(const char *journal_path);

/**
 * Flush the internal writer. This should be called after each block of work.
 * @return True on success
 */
bool bsg_ctj_flush(void);

/**
 * Write a journal entry to clear the value at path.
 * @param path The path whose value to clear.
 * @return True on success
 */
bool bsg_ctj_clear_value(const char *path);

/**
 * Set a double value at path.
 * @param path The path to set.
 * @param value The value to set.
 * @return True on success
 */
bool bsg_ctj_set_double(const char *path, double value);

/**
 * Set a boolean value at path.
 * @param path The path to set.
 * @param value The value to set.
 * @return True on success
 */
bool bsg_ctj_set_boolean(const char *path, bool value);

/**
 * Set a string value at path.
 * @param path The path to set.
 * @param value The value to set.
 * @return True on success
 */
bool bsg_ctj_set_string(const char *path, const char *value);

/**
 * Set an empty list at path.
 * @param path The path to set.
 * @return True on success
 */
bool bsg_ctj_set_empty_list(const char *path);

/**
 * Set an empty map at path.
 * @param path The path to set.
 * @return True on success
 */
bool bsg_ctj_set_empty_map(const char *path);

#endif
