#ifndef BUGSNAG_CTJOURNAL_H
#define BUGSNAG_CTJOURNAL_H

#include "event.h"
#include <stdbool.h>

/**
 * Initialize the crash-time journal system.
 *
 * @param journal_path /path/to/myjournal.journal.crashtime
 * @return True on success
 */
bool bsg_ctjournal_init(const char *journal_path);

/**
 * Write a journal entry to clear the value at path.
 * @param path The path whose value to clear.
 * @return True on success
 */
bool bsg_ctjournal_clear_value(const char *path);

/**
 * Set a double value at path.
 * @param path The path to set.
 * @param value The value to set.
 * @return True on success
 */
bool bsg_ctjournal_set_double(const char *path, double value);

/**
 * Set a boolean value at path.
 * @param path The path to set.
 * @param value The value to set.
 * @return True on success
 */
bool bsg_ctjournal_set_boolean(const char *path, bool value);

/**
 * Set a string value at path.
 * @param path The path to set.
 * @param value The value to set.
 * @return True on success
 */
bool bsg_ctjournal_set_string(const char *path, const char *value);

/**
 * Clear an entire metadata section.
 * @param section The section to clear.
 * @return True on success
 */
bool bsg_ctjournal_metadata_clear_section(const char *section);

/**
 * Clear a metadata value from a section.
 * @param section The section to clear from.
 * @param name The name of the value to clear.
 * @return True on success
 */
bool bsg_ctjournal_metadata_clear_value(const char *section, const char *name);

/**
 * Set a boolean metadata value.
 * @param section The section to set a value inside.
 * @param name The name of the value to set.
 * @param value The value to set it to.
 * @return True on success
 */
bool bsg_ctjournal_metadata_set_boolean(const char *section, const char *name,
                                        bool value);

/**
 * Set a double metadata value.
 * @param section The section to set a value inside.
 * @param name The name of the value to set.
 * @param value The value to set it to.
 * @return True on success
 */
bool bsg_ctjournal_metadata_set_double(const char *section, const char *name,
                                       double value);

/**
 * Set a string metadata value.
 * @param section The section to set a value inside.
 * @param name The name of the value to set.
 * @param value The value to set it to.
 * @return True on success
 */
bool bsg_ctjournal_metadata_set_string(const char *section, const char *name,
                                       const char *value);

/**
 * Set the current user information
 * @param id The user's ID
 * @param email The user's email
 * @param name The user's name
 * @return True on success
 */
bool bsg_ctjournal_set_user(const char *id, const char *email,
                            const char *name);

/**
 * Add a breadcrumb
 * @param breadcrumb The breadcrumb to add
 * @return True on success
 */
bool bsg_ctjournal_add_breadcrumb(const bugsnag_breadcrumb *breadcrumb);

#endif
