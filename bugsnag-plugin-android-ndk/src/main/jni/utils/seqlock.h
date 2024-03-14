//
// Created by Jason Morris on 02/02/2023.
//

#ifndef BUGSNAG_ANDROID_SEQLOCK_H
#define BUGSNAG_ANDROID_SEQLOCK_H

#include <stdatomic.h>
#include <stdbool.h>

#ifdef __cplusplus
extern "C" {
#endif

typedef atomic_uint_fast64_t bsg_seqlock_t;
typedef uint_fast64_t bsg_seqlock_status_t;

void bsg_seqlock_init(bsg_seqlock_t *lock);

void bsg_seqlock_acquire_write(bsg_seqlock_t *lock);

void bsg_seqlock_release_write(bsg_seqlock_t *lock);

bsg_seqlock_status_t bsg_seqlock_optimistic_read(bsg_seqlock_t *lock);

bool bsg_seqlock_validate(bsg_seqlock_t *lock, bsg_seqlock_status_t expected);

#define bsg_seqlock_is_write_locked(c) (((c) & 1uLL) != 0)

#ifdef __cplusplus
}
#endif

#endif // BUGSNAG_ANDROID_SEQLOCK_H
