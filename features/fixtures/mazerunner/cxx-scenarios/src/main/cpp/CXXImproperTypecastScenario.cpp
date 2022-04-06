#include <cstdint>
#include <cstdio>
#include <jni.h>

typedef struct {
  char *text;
} weird_obj_t;

char *__attribute__((optnone)) crash_improper_cast(void *counter) {
  weird_obj_t *obj = (weird_obj_t *)counter;

  return obj->text;
}

extern "C" {

JNIEXPORT void JNICALL
Java_com_bugsnag_android_mazerunner_scenarios_CXXImproperTypecastScenario_crash(
    JNIEnv *env, jobject instance) {
  printf("This one here: %s\n", crash_improper_cast((void *)39));
}
}
