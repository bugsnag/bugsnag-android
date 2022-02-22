#include <jni.h>
#include <stdbool.h>

#ifdef __cplusplus
extern "C" {
#endif

#include <fcntl.h>
#include <sys/stat.h>
#include <unistd.h>

static const char *su_paths[] = {
        // Common binaries
        "/system/xbin/su",
        "/system/bin/su",
        // < Android 5.0
        "/system/app/Superuser.apk",
        "/system/app/SuperSU.apk",
        // >= Android 5.0
        "/system/app/Superuser",
        "/system/app/SuperSU",
        // Fallback
        "/system/xbin/daemonsu",
        // Systemless root
        "/su/bin"
};
static const int su_paths_count = sizeof(su_paths) / sizeof(*su_paths);

static const char *should_not_be_writable[] = {
        "/sbin",
};
static const int should_not_be_writable_count = sizeof(should_not_be_writable) / sizeof(*should_not_be_writable);

static const char *should_not_be_creatable[] = {
        "/system/bugsnag_test_file.txt",
};
static const int should_not_be_creatable_count = sizeof(should_not_be_creatable) / sizeof(*should_not_be_creatable);

static inline int get_mode(const char* path) {
  struct stat st;
  if (lstat(path, &st) < 0) {
    return -1;
  }
  return st.st_mode;
}

static inline bool does_path_exist(const char* path) {
  return get_mode(path) >= 0;
}

static inline bool is_path_writable(const char* path) {
  int mode = get_mode(path);
  if (mode < 0) {
    return false;
  }
  return (mode & 2) != 0;
}

static inline bool can_create_file(const char* path) {
  unlink(path);

  const int fd = open(path, O_CREAT, O_RDONLY);
  if (fd < 0) {
    return false;
  }

  close(fd);
  unlink(path);
  return true;
}

static bool assert_no_su_paths() {
  for (int i = 0; i < su_paths_count; i++) {
    if (does_path_exist(su_paths[i])) {
      return false;
    }
  }
  return true;
}

static bool assert_no_writable_paths() {
  for (int i = 0; i < should_not_be_writable_count; i++) {
    if (is_path_writable(should_not_be_writable[i])) {
      return false;
    }
  }
  return true;
}

static bool assert_no_creatable_files() {
  for (int i = 0; i < should_not_be_creatable_count; i++) {
    if (can_create_file(should_not_be_creatable[i])) {
      return false;
    }
  }
  return true;
}

static bool is_rooted() {
  if (!assert_no_su_paths()) {
    return true;
  }
  if (!assert_no_writable_paths()) {
    return true;
  }
  if (!assert_no_creatable_files()) {
    return true;
  }
  return false;
}

#include <stdio.h>
JNIEXPORT jboolean JNICALL
Java_com_bugsnag_android_RootDetector_performNativeRootChecks(JNIEnv *env, jobject thiz) {
  return is_rooted();
}

#ifdef __cplusplus
}
#endif
