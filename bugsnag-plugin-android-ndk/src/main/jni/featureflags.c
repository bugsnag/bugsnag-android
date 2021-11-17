#include <string.h>

#include "bugsnag_ndk.h"
#include "featureflags.h"
#include "utils/string.h"

/*
 * Implementation notes:
 *
 * We store Feature Flags in a dynamically allocated array, sorted by the
 * feature flag 'name' string, allowing us to binary-search the array for
 * duplicates.
 *
 * This provides a reasonable compromise between speed and size, since the array
 * is no larger than the number of feature flags (not counting malloc padding)
 * and is unlikely to be modified often. It also keeps testing simple, as the
 * keys will always be in a known order.
 *
 * We resize the array using 'realloc' and 'memmove' to try and keep the
 * overhead reasonable.
 */

static int feature_flag_index(const bugsnag_event *env, const char *name) {
  // simple binary search for a feature-flag by name
  int low = 0;
  int high = env->feature_flag_count - 1;

  while (low <= high) {
    int mid = (low + high) >> 1;
    int cmp = strcmp(env->feature_flags[mid].name, name);

    if (cmp < 0) {
      low = mid + 1;
    } else if (cmp > 0) {
      high = mid - 1;
    } else {
      return mid; // found it
    }
  }

  return -(low + 1);
}

static void *grow_array_for_index(void *array, const size_t element_count,
                                  const size_t element_size,
                                  const unsigned int index) {
  void *new_array = realloc(array, (element_count + 1) * element_size);

  if (!new_array) {
    return NULL;
  }

  // if we need to: shift the "end" of the array by 1 element so 'index' has a
  // space
  memmove(new_array + ((index + 1) * element_size),
          new_array + (index * element_size),
          (element_count - index) * element_size);

  return new_array;
}

void bsg_set_feature_flag(bugsnag_event *env, const char *name,
                          const char *variant) {
  int expected_index = feature_flag_index(env, name);

  if (expected_index >= 0) {
    // feature flag already exists, so we overwrite the variant
    bsg_feature_flag *flag = &env->feature_flags[expected_index];

    // make sure we release the existing variant, if one exists
    free(flag->variant);

    if (variant) {
      // make a copy of the variant, so that the JVM can have it's memory back
      flag->variant = strdup(variant);
    } else {
      flag->variant = NULL;
    }
  } else {
    int new_flag_index = -expected_index - 1;

    // this is a new feature flag, we need to insert it - which means we also
    // need a new array
    bsg_feature_flag *new_flags =
        grow_array_for_index(env->feature_flags, env->feature_flag_count,
                             sizeof(bsg_feature_flag), new_flag_index);

    // we cannot grow the feature flag array, so we return
    if (!new_flags) {
      return;
    }

    new_flags[new_flag_index].name = strdup(name);

    if (variant) {
      new_flags[new_flag_index].variant = strdup(variant);
    } else {
      new_flags[new_flag_index].variant = NULL;
    }

    env->feature_flags = new_flags;
    env->feature_flag_count = env->feature_flag_count + 1;
  }
}

void bsg_clear_feature_flag(bugsnag_event *env, const char *name) {
  int flag_index = feature_flag_index(env, name);

  if (flag_index < 0) {
    // no such feature flag - early exit
    return;
  }

  bsg_feature_flag *flag = &env->feature_flags[flag_index];

  // release the memory held for name and possibly the variant
  free(flag->name);
  free(flag->variant);

  // pack the array elements down to fill in the "gap"
  // we don't resize the array down by one, that gets handled when elements are
  // added
  memmove(&env->feature_flags[flag_index], &env->feature_flags[flag_index + 1],
          (env->feature_flag_count - flag_index - 1) *
              sizeof(bsg_feature_flag));

  // mark the array as having one-less flag
  env->feature_flag_count = env->feature_flag_count - 1;
}

void bsg_free_feature_flags(bugsnag_event *env) {
  for (int index = 0; index < env->feature_flag_count; index++) {
    free(env->feature_flags[index].name);
    free(env->feature_flags[index].variant);
  }

  free(env->feature_flags);

  env->feature_flags = NULL;
  env->feature_flag_count = 0;
}
