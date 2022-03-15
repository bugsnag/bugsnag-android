#include <jni.h>
#include <cstdlib>
#include <cstdio>

static char * __attribute__((used)) somefakefunc(void) {
  return NULL;
};

int crash_write_read_only_mem(int counter) {
  if (counter > 2) {
    int *pointer = (int *)&somefakefunc;
    *pointer = counter;
  }
  return counter / 14;
}

extern "C" {

JNIEXPORT void JNICALL
Java_com_bugsnag_android_mazerunner_scenarios_CXXWriteReadOnlyMemoryScenario_crash(JNIEnv *env,
                                                                                   jobject instance) {
    printf("This one here: %d\n", crash_write_read_only_mem(42));
}

}
