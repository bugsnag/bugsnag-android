#ifndef BUGSNAG_UTILS_STRING_H
#define BUGSNAG_UTILS_STRING_H
#include "build.h"

#include <stdlib.h>

#ifdef __cplusplus
extern "C" {
#endif

/**
 * Copy a string from src to dst to a maximum of dst_size bytes, returning dst.
 *
 * This function behaves the same as strncpy, except:
 * - If src is longer than dst_size, it will copy as much as can fit and then
 *   set dst[dst_size-1] to 0.
 * - If src is NULL, dst[0] will be set to 0.
 */
char *bsg_strncpy_safe(char *dst, const char *src, size_t dst_size);
#ifdef __cplusplus
}
#endif
#endif
