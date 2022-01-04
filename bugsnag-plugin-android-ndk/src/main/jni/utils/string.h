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
 * Copy a maximum number of bytes from src to dst
 */
void bsg_strncpy(char *dst, const char *src, size_t len) __asyncsafe;

#ifdef __cplusplus
}
#endif
#endif
