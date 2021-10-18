//
// Created by Karl Stenerud on 07.10.21.
//
// Path builder is an async-safe, stack-based path builder system for journal
// paths. With it, you can progressively enter and leave path levels in the
// journal document you are building by stacking and unstacking levels.
//
// Note: Path builder is statically allocated, and must only be used from a
// single thread.

#ifndef BUGSNAG_ANDROID_PATH_BUILDER_H
#define BUGSNAG_ANDROID_PATH_BUILDER_H

#include <stdint.h>

#ifdef __cplusplus
extern "C" {
#endif

/**
 * Reset the path builder, clearing the path and all subpaths.
 */
void bsg_pb_reset(void);

/**
 * Stack a string (object) level to the current path.
 * Stacking too deep (>100 levels) or building a path that's too long (>500
 * bytes) will cause this function to no-op.
 *
 * @param key The map key for the new path level.
 */
void bsg_pb_stack_map_key(const char *key);

/**
 * Stack an integer (list) level to the current path.
 * Stacking too deep (>100 levels) or building a path that's too long (>500
 * bytes) will cause this function to no-op.
 *
 * @param index The list index for the new path level.
 */
void bsg_pb_stack_list_index(int64_t index);

/**
 * Stack the last list entry of the current list to the current path.
 * Stacking too deep (>100 levels) or building a path that's too long (>500
 * bytes) will cause this function to no-op.
 *
 * @param index The list index for the new path level.
 */
void bsg_pb_stack_last_list_index();

/**
 * Stack a new list entry to the current path (such that the path ends in a
 * dot). Stacking too deep (>100 levels) or building a path that's too long
 * (>500 bytes) will cause this function to no-op.
 */
void bsg_pb_stack_new_list_entry(void);

/**
 * Unstack the current subpath level, returning to the next higher subpath
 * level.
 */
void bsg_pb_unstack(void);

/**
 * Get the current full path.
 *
 * @return The current path.
 */
const char *bsg_pb_path(void);

#ifdef __cplusplus
}
#endif

#endif // BUGSNAG_ANDROID_PATH_BUILDER_H
