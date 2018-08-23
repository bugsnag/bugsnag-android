#include <jni.h>
#include <stdlib.h>
#include <signal.h>

extern "C" {
    JNIEXPORT void JNICALL Java_com_bugsnag_android_mazerunner_scenarios_CXXReadWriteOnlyPageScenario_crashWithSIGBUS (JNIEnv *env, jobject instance);

  JNIEXPORT void JNICALL Java_com_bugsnag_android_ndk_test_MainActivity_causeCppFpe (JNIEnv *env, jobject instance);
  JNIEXPORT void JNICALL Java_com_bugsnag_android_ndk_test_MainActivity_causeCppNpe (JNIEnv *env, jobject instance);
  JNIEXPORT void JNICALL Java_com_bugsnag_android_ndk_test_MainActivity_causeCppAbort (JNIEnv *env, jobject instance);
  JNIEXPORT void JNICALL Java_com_bugsnag_android_ndk_test_MainActivity_causeCppTrap (JNIEnv *env, jobject instance);
  JNIEXPORT void JNICALL Java_com_bugsnag_android_ndk_test_MainActivity_causeCppIll (JNIEnv *env, jobject instance);
}

static void __attribute__((used)) somefakefunc(void) {}

typedef struct {
    int field1;
    char *field2;
} reporter_t;

int crash_abort() {
    abort();
    return 7;
}

int crash_floating_point() {
    int i = 0;
    int j = 34 / i;

    return j;
}

int crash_null_pointer() {
    int *i = NULL;
    int j = 34 / *i;

    return j;
}

int crash_released_obj() {
    reporter_t *report = (reporter_t *) malloc(sizeof(reporter_t));
    report->field1 = 6;
    free(report);

    return report->field1;
}

int crash_undefined_inst() {
#if __i386__
    __asm__ volatile("ud2" : : :);
#elif __x86_64__
    __asm__ volatile("ud2" : : :);
#elif __arm__ && __ARM_ARCH == 6 && __thumb__
    __asm__ volatile(".word 0xde00" : : :);
#elif __arm__ && __ARM_ARCH == 6
    __asm__ volatile(".long 0xf7f8a000" : : :);
#elif __arm64__
    __asm__ volatile(".long 0xf7f8a000" : : :);
#endif
    return 42;
}

int crash_write_read_only() {
    // Write to a read-only page
    volatile char *ptr = (char *)somefakefunc;
    *ptr = 0;

    return 5;
}

int crash_trap() {
    __builtin_trap();

    return 0;
}

int crash_priv_inst() {
// execute a privileged instruction
#if __i386__
    __asm__ volatile("hlt" : : :);
#elif __x86_64__
    __asm__ volatile("hlt" : : :);
#elif __arm__ && __ARM_ARCH == 7
    __asm__ volatile(".long 0xe1400070" : : :);
#elif __arm__ && __ARM_ARCH == 6 && __thumb__
    __asm__ volatile(".long 0xf5ff8f00" : : :);
#elif __arm__ && __ARM_ARCH == 6
    __asm__ volatile(".long 0xe14ff000" : : :);
#elif __arm64__
    __asm__ volatile("tlbi alle1" : : :);
#endif
    return 5;
}

int crash_stack_overflow() {
    crash_stack_overflow();

    return 4;
}



JNIEXPORT void JNICALL
Java_com_bugsnag_android_ndk_test_MainActivity_causeCppFpe(JNIEnv *env, jobject instance) {
    crash_floating_point();
}

JNIEXPORT void JNICALL
Java_com_bugsnag_android_ndk_test_MainActivity_causeCppNpe(JNIEnv *env, jobject instance) {
    crash_null_pointer();
}

JNIEXPORT void JNICALL
Java_com_bugsnag_android_mazerunner_scenarios_CXXReadWriteOnlyPageScenario_crashWithSIGBUS(JNIEnv *env, jobject instance) {
    crash_write_read_only();
}

JNIEXPORT void JNICALL
Java_com_bugsnag_android_ndk_test_MainActivity_causeCppAbort(JNIEnv *env, jobject instance) {
    crash_abort();
}

JNIEXPORT void JNICALL
Java_com_bugsnag_android_ndk_test_MainActivity_causeCppTrap(JNIEnv *env, jobject instance) {
    crash_trap();
}

JNIEXPORT void JNICALL
Java_com_bugsnag_android_ndk_test_MainActivity_causeCppIll(JNIEnv *env, jobject instance) {
    crash_priv_inst();
}




