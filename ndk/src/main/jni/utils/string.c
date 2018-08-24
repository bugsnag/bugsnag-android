#include "string.h"
#include <stdbool.h>
#include <limits.h>

void bsg_strcpy(char *dst, char *src) {
  int i = 0;
  while (true) {
    char current = src[i];
    dst[i] = current;
    if (current == '\0') {
      break;
    } else if (i == INT_MAX) {
      break;
    }
    i++;
  }
}

size_t bsg_strlen(char *str) __asyncsafe {
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
