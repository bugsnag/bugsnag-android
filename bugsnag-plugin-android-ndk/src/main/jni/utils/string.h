#ifndef BUGSNAG_UTILS_STRING_H
#define BUGSNAG_UTILS_STRING_H

#include "build.h"
#include <stdlib.h>

#ifdef __cplusplus
extern "C" {
#endif

/**
 * Return the length of a string, or 0 if the pointer is NULL.
 */
size_t bsg_strlen(const char *str) __asyncsafe;

/**
 * Copy a string from src to dst, null padding the rest
 */
void bsg_strncpy_safe(char *dst, const char *src, int dst_size);

#ifdef __cplusplus
}
#endif
#endif
