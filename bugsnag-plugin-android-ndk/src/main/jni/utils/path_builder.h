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

#ifdef __cplusplus
extern "C" {
#endif

/**
 * Reset the path builder, clearing the path and all subpaths.
 */
void bsg_pb_reset(void);

/**
 * Stack a string (object) level to the current path.
 *
 * @param value The string to use as the map key for the new path level.
 */
void bsg_pb_stack_string(const char *value);

/**
 * Stack an integer (array) level to the current path.
 *
 * @param value The integer to use as the array index for the new path level.
 */
void bsg_pb_stack_int(int value);

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
