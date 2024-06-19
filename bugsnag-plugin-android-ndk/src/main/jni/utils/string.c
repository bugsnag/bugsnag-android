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

size_t bsg_strncpy(char *dst, const char *src, size_t dst_size) {
  if (dst == NULL || dst_size == 0) {
    return 0;
  }
  dst[0] = '\0';
  if (src != NULL) {
    return strlcat(dst, src, dst_size);
  }
  return 0;
}

void bsg_hex_encode(char *dst, const void *src, size_t byte_count,
                    size_t max_chars) __asyncsafe {
  static const char *hex = "0123456789abcdef";

  const size_t byte_copy_count =
      (max_chars > byte_count * 2) ? byte_count : (max_chars - 1) / 2;

  char *cursor = (char *)src;
  char *outCursor = dst;
  for (size_t i = 0; i < byte_copy_count; ++i) {
    *outCursor++ = hex[(*cursor >> 4) & 0xF];
    *outCursor++ = hex[(*cursor++) & 0xF];
  }

  *outCursor = '\0';
}