#ifndef BUGSNAG_UTILS_STRING_H
#define BUGSNAG_UTILS_STRING_H

#ifdef __cplusplus
extern "C" {
#endif

#include <string.h>

/**
 * Copy the contents of src to dst where src is null-terminated
 */
void bsg_strncpy(char *dst, char *src, size_t len);

#ifdef __cplusplus
}
#endif
#endif
