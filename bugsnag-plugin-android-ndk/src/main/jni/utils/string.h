#ifndef BUGSNAG_UTILS_STRING_H
#define BUGSNAG_UTILS_STRING_H
#include "build.h"

#include <stdlib.h>

#ifdef __cplusplus
extern "C" {
#endif

/**
 * Copy the contents of src to dst where src is null-terminated
 */
void bsg_strcpy(char *dst, char *src) __asyncsafe;

/**
 * Return the length of a string
 */
size_t bsg_strlen(const char *str) __asyncsafe;

/**
 * Copy a maximum number of bytes from src to dst
 */
void bsg_strncpy(char *dst, char *src, size_t len) __asyncsafe;

/**
 * Copy a string from src to dst, null padding the rest
 */
void bsg_strncpy_safe(char *dst, const char *src, int dst_size);
#ifdef __cplusplus
}
#endif
#endif
