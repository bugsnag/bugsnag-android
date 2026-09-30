#include <fcntl.h>
#include <jni.h>
#include <stdbool.h>
#include <stdio.h>
#include <string.h>
#include <sys/stat.h>
#include <unistd.h>

#ifdef __cplusplus
extern "C" {
#endif

static const char *su_paths[] = {
        // Common binaries
        "/system/xbin/su",
        "/system/bin/su",
        "/sbin/su",
        // < Android 5.0
        "/system/app/Superuser.apk",
        "/system/app/SuperSU.apk",
        "/data/local/xbin/su",
        "/data/local/bin/su",
        "/data/local/su",
        "/su/bin/su",
        "/system/sd/xbin/su",
        "/system/bin/failsafe/su",
        "/vendor/bin/su",
        "/odm/bin/su",
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

static inline int get_mode(const char *path) {
  struct stat st;
  if (stat(path, &st) < 0) {
    return -1;
  }
  return (int) st.st_mode;
}

static inline bool does_path_exist(const char *path) {
  return get_mode(path) >= 0;
}

static inline bool is_path_writable(const char *path) {
  int mode = get_mode(path);
  if (mode < 0) {
    return false;
  }
  return (mode & 2) != 0;
}

static inline bool can_create_file(const char *path) {
  unlink(path);

  const int fd = open(path, O_CREAT | O_RDWR | O_TRUNC, 0600);
  if (fd < 0) {
    return false;
  }

  const bool can_set_permissions = fchmod(fd, 0600) == 0;
  close(fd);
  unlink(path);
  return can_set_permissions;
}

static inline bool line_matches_root_property(const char *line) {
  char compact[1024];
  size_t out = 0;

  for (size_t i = 0; line[i] != '\0' && out < sizeof(compact) - 1; i++) {
    const char c = line[i];
    if (c != ' ' && c != '\t' && c != '\n' && c != '\r' && c != '\f' && c != '\v') {
      compact[out++] = c;
    }
  }

  compact[out] = '\0';
  return strncmp(compact, "ro.debuggable=[1]", 17) == 0 || strncmp(compact, "ro.secure=[0]", 13) == 0;
}

static bool does_build_prop_indicate_root(const char *path) {
  if (path == NULL) {
    return false;
  }

  FILE *file = fopen(path, "r");
  if (file == NULL) {
    return false;
  }

  char line[1024];
  bool rooted = false;
  while (fgets(line, sizeof(line), file) != NULL) {
    if (line_matches_root_property(line)) {
      rooted = true;
      break;
    }
  }

  fclose(file);
  return rooted;
}

static bool does_root_binary_list_exist(JNIEnv *env, jobject rootBinaryLocations) {
  if (rootBinaryLocations == NULL) {
    return false;
  }

  jclass listClass = (*env)->GetObjectClass(env, rootBinaryLocations);
  if (listClass == NULL) {
    return false;
  }

  jmethodID sizeMethod = (*env)->GetMethodID(env, listClass, "size", "()I");
  jmethodID getMethod = (*env)->GetMethodID(env, listClass, "get", "(I)Ljava/lang/Object;");
  if (sizeMethod == NULL || getMethod == NULL) {
    (*env)->DeleteLocalRef(env, listClass);
    return false;
  }

  jint size = (*env)->CallIntMethod(env, rootBinaryLocations, sizeMethod);
  if ((*env)->ExceptionCheck(env)) {
    (*env)->ExceptionClear(env);
    (*env)->DeleteLocalRef(env, listClass);
    return false;
  }

  for (jint i = 0; i < size; i++) {
    jobject candidate = (*env)->CallObjectMethod(env, rootBinaryLocations, getMethod, i);
    if ((*env)->ExceptionCheck(env)) {
      (*env)->ExceptionClear(env);
      (*env)->DeleteLocalRef(env, listClass);
      return false;
    }

    if (candidate == NULL) {
      continue;
    }

    const char *candidatePath = (*env)->GetStringUTFChars(env, (jstring) candidate, NULL);
    if (candidatePath != NULL) {
      const bool exists = does_path_exist(candidatePath);
      (*env)->ReleaseStringUTFChars(env, (jstring) candidate, candidatePath);
      if (exists) {
        (*env)->DeleteLocalRef(env, candidate);
        (*env)->DeleteLocalRef(env, listClass);
        return true;
      }
    }

    (*env)->DeleteLocalRef(env, candidate);
  }

  (*env)->DeleteLocalRef(env, listClass);
  return false;
}

static bool does_root_binary_list_exist_default(void) {
  for (int i = 0; i < su_paths_count; i++) {
    if (does_path_exist(su_paths[i])) {
      return true;
    }
  }
  return false;
}


static bool has_writable_system_or_creatable_file(void) {
  for (int i = 0; i < should_not_be_writable_count; i++) {
    if (is_path_writable(should_not_be_writable[i])) {
      return true;
    }
  }

  for (int i = 0; i < should_not_be_creatable_count; i++) {
    if (can_create_file(should_not_be_creatable[i])) {
      return true;
    }
  }

  return false;
}

JNIEXPORT jboolean JNICALL
Java_com_bugsnag_android_RootDetector_nativeCheckRootIndicators(JNIEnv *env, jobject thiz) {
  jclass detectorClass = (*env)->GetObjectClass(env, thiz);
  if (detectorClass == NULL) {
    return does_root_binary_list_exist_default() || does_build_prop_indicate_root("/system/build.prop") || has_writable_system_or_creatable_file();
  }

  jfieldID rootBinaryLocationsField = (*env)->GetFieldID(env, detectorClass, "rootBinaryLocations", "Ljava/util/List;");
  jfieldID buildPropsField = (*env)->GetFieldID(env, detectorClass, "buildProps", "Ljava/io/File;");
  if (rootBinaryLocationsField == NULL || buildPropsField == NULL) {
    (*env)->DeleteLocalRef(env, detectorClass);
    return does_root_binary_list_exist_default() || does_build_prop_indicate_root("/system/build.prop") || has_writable_system_or_creatable_file();
  }

  jobject rootBinaryLocations = (*env)->GetObjectField(env, thiz, rootBinaryLocationsField);
  jobject buildProps = (*env)->GetObjectField(env, thiz, buildPropsField);

  bool rooted = false;
  if (rootBinaryLocations != NULL) {
    rooted = does_root_binary_list_exist(env, rootBinaryLocations);
    (*env)->DeleteLocalRef(env, rootBinaryLocations);
  }

  if (!rooted && buildProps != NULL) {
    jclass fileClass = (*env)->GetObjectClass(env, buildProps);
    if (fileClass != NULL) {
      jmethodID getPathMethod = (*env)->GetMethodID(env, fileClass, "getPath", "()Ljava/lang/String;");
      if (getPathMethod != NULL) {
        jstring buildPropsPath = (jstring) (*env)->CallObjectMethod(env, buildProps, getPathMethod);
        if (!(*env)->ExceptionCheck(env) && buildPropsPath != NULL) {
          const char *path = (*env)->GetStringUTFChars(env, buildPropsPath, NULL);
          if (path != NULL) {
            rooted = does_build_prop_indicate_root(path);
            (*env)->ReleaseStringUTFChars(env, buildPropsPath, path);
          }
          (*env)->DeleteLocalRef(env, buildPropsPath);
        } else if ((*env)->ExceptionCheck(env)) {
          (*env)->ExceptionClear(env);
        }
      }
      (*env)->DeleteLocalRef(env, fileClass);
    }
    (*env)->DeleteLocalRef(env, buildProps);
    buildProps = NULL;
  }

  if (buildProps != NULL) {
    (*env)->DeleteLocalRef(env, buildProps);
  }

  if (!rooted) {
    rooted = has_writable_system_or_creatable_file();
  }

  (*env)->DeleteLocalRef(env, detectorClass);
  return rooted ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jboolean JNICALL
Java_com_bugsnag_android_RootDetector_nativeCheckRootBinaries(JNIEnv *env, jobject thiz, jobject rootBinaryLocations) {
  (void) thiz;
  if (rootBinaryLocations == NULL) {
    return does_root_binary_list_exist_default() ? JNI_TRUE : JNI_FALSE;
  }
  return does_root_binary_list_exist(env, rootBinaryLocations) ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jboolean JNICALL
Java_com_bugsnag_android_RootDetector_nativeCheckBuildProps(JNIEnv *env, jobject thiz, jstring buildPropPath) {
  (void) thiz;

  if (buildPropPath == NULL) {
    return does_build_prop_indicate_root("/system/build.prop") ? JNI_TRUE : JNI_FALSE;
  }

  const char *path = (*env)->GetStringUTFChars(env, buildPropPath, NULL);
  if (path == NULL) {
    return JNI_FALSE;
  }

  const bool rooted = does_build_prop_indicate_root(path);
  (*env)->ReleaseStringUTFChars(env, buildPropPath, path);
  return rooted ? JNI_TRUE : JNI_FALSE;
}

#ifdef __cplusplus
}
#endif
