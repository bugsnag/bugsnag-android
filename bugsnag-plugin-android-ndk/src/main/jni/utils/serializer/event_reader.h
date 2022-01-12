#pragma once

#include <event.h>

extern const int BSG_MIGRATOR_CURRENT_VERSION;

/**
 * Read an event from a file path, converting from older formats if needed.
 *
 * The report version is serialized in the file header, and old formats are
 * maintained in migrate.h for backwards compatibility. These are then migrated
 * to the current bugsnag_event struct.
 *
 * @param filepath  A full path to a file
 *
 * @return An allocated event or NULL if no event could be read
 */
bugsnag_event *bsg_read_event(char *filepath);
