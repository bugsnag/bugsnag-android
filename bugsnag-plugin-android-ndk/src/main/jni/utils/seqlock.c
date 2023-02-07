#include "seqlock.h"

/*
 * Similar to a Sequence Lock (seqlock) in the Linux Kernel, implemented as a
 * pure-spinning lock.
 *
 * When the value of the lock (counter) is odd then a write is in progress. This
 * allows us to also perform optimistic-reads without waiting for the lock.
 *
 * This implementation does *not* support blocking reads, and will not block
 * optimistic reads when a write is in-progress. This allows for reads of
 * invalid/stale data within a signal handler boundary.
 */

#ifndef spin

#if (defined(__i386__) || defined(__amd64__))
#define spin() __builtin_ia32_pause()
#elif (defined(__aarch64__) || defined(__arm__))
#define spin() asm inline("yield")
#else
#define spin()
#endif

#endif // #ifndef spin

void bsg_seqlock_init(bsg_seqlock_t *lock) { atomic_init(lock, 0uLL); }

void bsg_seqlock_acquire_write(bsg_seqlock_t *lock) {
  atomic_thread_fence(memory_order_acquire);
  bsg_seqlock_status_t current = atomic_load(lock);

  for (;;) {
    if (bsg_seqlock_is_write_locked(current)) {
      spin();
    } else if (atomic_compare_exchange_strong(lock, &current, current + 1)) {
      break;
    }
  }
}

void bsg_seqlock_release_write(bsg_seqlock_t *lock) {
  bsg_seqlock_status_t current = atomic_load(lock);

  for (;;) {
    if (!bsg_seqlock_is_write_locked(current)) {
      break;
    } else if (atomic_compare_exchange_strong(lock, &current, current + 1)) {
      break;
    }
  }

  atomic_thread_fence(memory_order_release);
}

bsg_seqlock_status_t bsg_seqlock_optimistic_read(bsg_seqlock_t *lock) {
  bsg_seqlock_status_t status = atomic_load(lock);
  // optimistic reads are never valid during a write, and we return 0 in these
  // cases validate will always return `false` in these cases
  return bsg_seqlock_is_write_locked(status) ? 0 : status;
}

bool bsg_seqlock_validate(bsg_seqlock_t *lock, bsg_seqlock_status_t expected) {
  atomic_thread_fence(memory_order_acquire);
  return atomic_load(lock) == expected;
}
