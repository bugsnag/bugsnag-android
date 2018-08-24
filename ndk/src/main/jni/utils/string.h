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
size_t bsg_strlen(char *str) __asyncsafe;

#ifdef __cplusplus
}
#endif
#endif
