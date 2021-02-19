#include "string.h"
#include <string.h>

char *bsg_strncpy_safe(char *dst, const char *src, size_t dst_size) {
  if (dst_size > 0) {
    if (src == NULL) {
      dst[0] = '\0';
    } else {
      strncpy(dst, src, dst_size);
      dst[dst_size - 1] = 0;
    }
  }
  return dst;
}
