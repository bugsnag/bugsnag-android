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
void bsg_strcpy(char *dst, const char *src) __asyncsafe;

/**
 * Return the length of a string, or 0 if the pointer is NULL.
 */
size_t bsg_strlen(const char *str) __asyncsafe;

/**
 * Copy a maximum number of bytes from src to dst
 */
size_t bsg_strncpy(char *dst, const char *src, size_t len) __asyncsafe;

/**
 * Encode a number of bytes into dst while hex encoding the data.
 *
 * @param dst the destination buffer, which have enough space for at least
 * max_chars
 * @param src pointer to the first byte to encode
 * @param byte_count the number of bytes to encode from src
 * @param max_chars the maximum number of chars to encode (including a
 * terminator)
 */
void bsg_hex_encode(char *dst, const void *src, size_t byte_count,
                    size_t max_chars) __asyncsafe;

#ifdef __cplusplus
}
#endif
#endif
