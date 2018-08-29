#include <jni.h>
#include <stdlib.h>
#include <signal.h>
#include <bugsnag.h>

extern "C" {

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

JNIEXPORT void JNICALL Java_com_bugsnag_android_example_ExampleActivity_crashWithSIGBUS(JNIEnv *env, jobject instance) {
    crash_write_read_only();
}

JNIEXPORT void JNICALL Java_com_bugsnag_android_example_ExampleActivity_notifyFromCXX(JNIEnv *env, jobject instance) {
    // Set the current user
    bugsnag_set_user_env(env, "124323", "joe mills", "j@ex.co");
    // Leave a breadcrumb
    bugsnag_leave_breadcrumb_env(env, "Critical failure", BSG_CRUMB_LOG);
    // Send an error report
    bugsnag_notify_env(env, "Oh no", "The mill!", BSG_SEVERITY_INFO);
}
}
