#include "string.h"
#include <limits.h>
#include <stdbool.h>
#include <string.h>

// Anything more than this and we shouldn't even be sending or using it.
const size_t STRING_MAX_LENGTH = 1024 * 1024 * 10;

size_t bsg_strlen(const char *str) {
  if (str == NULL) {
    return 0;
  }
  return strnlen(str, STRING_MAX_LENGTH);
}

void bsg_strncpy_safe(char *dst, const char *src, int dst_size) {
  if (src == NULL || dst == NULL || dst_size == 0) {
    return;
  }
  dst[0] = '\0';
  strncat(dst, src, dst_size - 1);
}
