#ifndef BUGSNAG_MIGRATE_H
#define BUGSNAG_MIGRATE_H

#include "event.h"

#ifdef __cplusplus
extern "C" {
#endif

/**
 * Check if the specified event version requires migration
 * @param event_version
 * @return true if this version requires migration
 */
bool bsg_event_requires_migration(int event_version);

/**
 * Read an event of the specified version from an FD and migrate it to the
 * latest structure.
 * @param event_version The version decoded from the event file header
 * @param fd The file descriptor after reading the header.
 * @return The event or NULL if the version is not handled.
 */
bugsnag_event *bsg_event_read_and_migrate(int event_version, int fd);

#ifdef __cplusplus
}
#endif
#endif
