#ifndef BUGSNAG_ANDROID_FEATUREFLAGS_H
#define BUGSNAG_ANDROID_FEATUREFLAGS_H

#include "bugsnag_ndk.h"

/**
 * Set a feature flag in the given `bugsnag_event` with an optional variant.
 * This function will overwrite any existing feature flag with the specified
 * name. Setting a `variant` to NULL is *not* the same as clearing the feature
 * flag, which must be done with `bsg_clear_feature_flag`.
 *
 * @param event the environment to populate with the given feature flag
 * @param name the name of the feature flag (may not be NULL)
 * @param variant if non-NULL: the variant to set the feature flag to
 */
void bsg_set_feature_flag(bugsnag_event *event, const char *name,
                          const char *variant);

/**
 * Release a specified feature flag from the given `bugsnag_event` if it
 * exists. If the flag does not exist this is a no-op and the `bugsnag_event`
 * will remain untouched. Any dynamic memory associated with the cleared feature
 * flag is correctly released.
 *
 * @param event the environment to clear the given feature flag in
 * @param name the name of the feature flag to clear
 */
void bsg_clear_feature_flag(bugsnag_event *event, const char *name);

/**
 * Release all of the memory for all of the feature flags stored in the given
 * `bugsnag_event`. This also zeroes the `bugsnag_event->feature_flag_count`
 * and cleans up the array pointers, effectively clearing all of the feature
 * flags.
 *
 * @param event the environment to remove all the feature flags from
 */
void bsg_free_feature_flags(bugsnag_event *event);

#endif // BUGSNAG_ANDROID_FEATUREFLAGS_H
