#include "string.h"

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
