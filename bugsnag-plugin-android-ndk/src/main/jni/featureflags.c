#include <string.h>

#include "bugsnag_ndk.h"
#include "featureflags.h"
#include "utils/seqlock.h"
#include "utils/string.h"

#ifdef __cplusplus
extern "C" {
#endif

/*
 * Implementation notes:
 *
 * We store Feature Flags in a dynamically allocated array, maintaining the
 * insertion order. Modifying an existing entry causes a reinsertion
 * (moving it to the end of the array).
 *
 * Searches are linear, but the impact should be negligible up to tens of
 * thousands of entries.
 *
 * This provides a reasonable compromise between speed and size, since the array
 * is no larger than the number of feature flags (not counting malloc padding)
 * and is unlikely to be modified often.
 *
 * We resize the array using 'realloc' and 'memmove' to try and keep the
 * overhead reasonable.
 */

#define INDEX_NOT_FOUND (-1)

bsg_seqlock_t bsg_feature_flag_lock;

static int index_of_flag_named(const bugsnag_event *const event,
                               const char *const name) {
  for (int i = 0; i < event->feature_flag_count; i++) {
    if (strcmp(event->feature_flags[i].name, name) == 0) {
      return i;
    }
  }
  return INDEX_NOT_FOUND;
}

static void remove_at_index_and_compact(bugsnag_event *const event,
                                        const int index) {
  if (event->feature_flag_count > 1 && index < event->feature_flag_count - 1) {
    memmove(&event->feature_flags[index], &event->feature_flags[index + 1],
            (event->feature_flag_count - index - 1) * sizeof(bsg_feature_flag));
  }
}

static void free_flag_contents(bsg_feature_flag *const flag) {
  free(flag->name);
  free(flag->variant);
}

static void set_flag_variant(bsg_feature_flag *const flag,
                             const char *const variant) {
  if (variant == NULL) {
    flag->variant = NULL;
  } else {
    flag->variant = strdup(variant);
  }
}

static void insert_new(bugsnag_event *const event, const char *const name,
                       const char *const variant) {
  bsg_feature_flag *new_flags =
      realloc(event->feature_flags, (event->feature_flag_count + 1) *
                                        sizeof(event->feature_flags[0]));
  if (!new_flags) {
    return;
  }
  event->feature_flags = new_flags;

  bsg_feature_flag *flag = &new_flags[event->feature_flag_count];
  flag->name = strdup(name);
  if (flag->name == NULL) {
    return;
  }
  set_flag_variant(flag, variant);

  event->feature_flag_count++;
}

static void modify_at_index(bugsnag_event *const event, const int index,
                            const char *const variant) {
  bsg_feature_flag *flag = &event->feature_flags[index];
  free(flag->variant);
  set_flag_variant(flag, variant);
}

void bsg_set_feature_flag(bugsnag_event *event, const char *const name,
                          const char *const variant) {
  bsg_seqlock_acquire_write(&bsg_feature_flag_lock);
  const int index = index_of_flag_named(event, name);
  if (index == INDEX_NOT_FOUND) {
    insert_new(event, name, variant);
  } else {
    modify_at_index(event, index, variant);
  }
  bsg_seqlock_release_write(&bsg_feature_flag_lock);
}

void bsg_clear_feature_flag(bugsnag_event *const event,
                            const char *const name) {
  bsg_seqlock_acquire_write(&bsg_feature_flag_lock);
  const int index = index_of_flag_named(event, name);
  if (index != INDEX_NOT_FOUND) {
    free_flag_contents(&event->feature_flags[index]);
    remove_at_index_and_compact(event, index);
    event->feature_flag_count--;
  }
  bsg_seqlock_release_write(&bsg_feature_flag_lock);
}

void bsg_free_feature_flags(bugsnag_event *const event) {
  bsg_seqlock_acquire_write(&bsg_feature_flag_lock);
  const size_t old_flag_count = event->feature_flag_count;
  bsg_feature_flag *old_flags = event->feature_flags;

  event->feature_flag_count = 0;
  event->feature_flags = NULL;
  bsg_seqlock_release_write(&bsg_feature_flag_lock);

  // we release the actual memory outside of the lock
  for (int index = 0; index < old_flag_count; index++) {
    free_flag_contents(&old_flags[index]);
  }

  free(old_flags);
}

#ifdef __cplusplus
}
#endif
