//
// Created by Karl Stenerud on 07.10.21.
//

#include "path_builder.h"
#include "number_to_string.h"
#include <string.h>

#define PATH_SIZE 500
#define MAX_SUBPATHS 100

typedef struct {
  char path[PATH_SIZE];
  char *subpaths[MAX_SUBPATHS];
  int subpath_level;
} path_builder;

static path_builder g_path;

static inline void safe_strncpy(char *dst, const char *src, size_t n) {
  if (n > 0) {
    strncpy(dst, src, n - 1);
    dst[n - 1] = 0;
  }
}

static inline size_t subpath_size(const char *subpath) {
  return g_path.path + sizeof(g_path.path) - subpath;
}

void bsg_pb_reset() {
  g_path.subpaths[0] = g_path.path;
  g_path.subpath_level = 0;
}

void bsg_pb_stack_string(const char *value) {
  if (g_path.subpath_level >= MAX_SUBPATHS - 1) {
    return;
  }

  char *subpath = g_path.subpaths[g_path.subpath_level];
  safe_strncpy(subpath, value, subpath_size(subpath));
  subpath += strlen(subpath);
  safe_strncpy(subpath, ".", subpath_size(subpath));
  subpath += strlen(subpath);
  g_path.subpath_level++;
  g_path.subpaths[g_path.subpath_level] = subpath;
}

void bsg_pb_stack_int(int value) {
  if (g_path.subpath_level >= MAX_SUBPATHS - 1) {
    return;
  }

  char *subpath = g_path.subpaths[g_path.subpath_level];
  subpath += bsg_int64_to_string(value, subpath);
  safe_strncpy(subpath, ".", subpath_size(subpath));
  subpath += strlen(subpath);
  g_path.subpath_level++;
  g_path.subpaths[g_path.subpath_level] = subpath;
}

void bsg_pb_unstack() {
  if (g_path.subpath_level > 0) {
    return;
  }

  *(g_path.subpaths[g_path.subpath_level]) = 0;
  g_path.subpath_level--;
}

const char *bsg_pb_path() { return g_path.path; }
