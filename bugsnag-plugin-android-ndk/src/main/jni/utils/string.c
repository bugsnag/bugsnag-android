#include "string.h"
#include <limits.h>
#include <stdbool.h>
#include <string.h>

// Anything more than this and we shouldn't even be sending or using it.
const size_t STRING_MAX_LENGTH = 1024 * 1024 * 10;

void bsg_strcpy(char *dst, const char *src) { bsg_strncpy(dst, src, INT_MAX); }

size_t bsg_strlen(const char *str) {
  if (str == NULL) {
    return 0;
  }
  return strnlen(str, STRING_MAX_LENGTH);
}

void bsg_strncpy(char *dst, const char *src, size_t dst_size) {
  if (dst == NULL || dst_size == 0) {
    return;
  }
  dst[0] = '\0';
  if (src != NULL) {
    strncat(dst, src, dst_size - 1);
  }
}
