#include "string.h"
#include <limits.h>
#include <stdbool.h>
#include <string.h>

void bsg_strcpy(char *dst, char *src) { bsg_strncpy(dst, src, INT_MAX); }

void bsg_strncpy(char *dst, char *src, size_t len) {
  int i = 0;
  while (i <= len) {
    char current = src[i];
    dst[i] = current;
    if (current == '\0') {
      break;
    }
    i++;
  }
}

size_t bsg_strlen(const char *str) {
  size_t i = 0;
  while (true) {
    char current = str[i];
    if (current == '\0') {
      return i;
    } else if (i == INT_MAX) {
      return INT_MAX;
    }
    i++;
  }
}

void bsg_strncpy_safe(char *dst, const char *src, int dst_size) {
  if (dst_size == 0)
    return;
  dst[0] = '\0';
  if (src != NULL) {
    strncat(dst, src, dst_size - 1);
  }
}
