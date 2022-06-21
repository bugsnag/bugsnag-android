#ifndef BUGSNAG_ANDROID_MEMORY_H
#define BUGSNAG_ANDROID_MEMORY_H

#ifdef __cplusplus
extern "C" {
#endif

/**
 * Crash-safe 'free' that won't cause deadlocks if a signal handler is active.
 * @param ptr
 */
void bsg_free(void *ptr);

#ifdef __cplusplus
}
#endif

#endif // BUGSNAG_ANDROID_MEMORY_H
