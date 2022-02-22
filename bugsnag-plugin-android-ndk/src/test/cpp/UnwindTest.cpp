#include <cstdio>
#include <jni.h>

#include <utils/stack_unwinder.h>

#ifdef __cplusplus
extern "C" {
#endif

typedef ssize_t (*unwinder)(bugsnag_stackframe *, siginfo *, void *);

ssize_t
bsg_unwind_crash_stack(bugsnag_stackframe stack[BUGSNAG_FRAMES_MAX],
                       siginfo_t *info, void *user_context);

ssize_t
bsg_unwind_concurrent_stack(bugsnag_stackframe stack[BUGSNAG_FRAMES_MAX],
                            siginfo_t *info, void *user_context);

// the following functions are marked as optnone to make stack contents
// assertions uniform across architectures, etc
// https://clang.llvm.org/docs/AttributeReference.html#optnone

jobject __attribute__((optnone)) unwind_func_four(JNIEnv *env, unwinder func) {
  bugsnag_stackframe stack[BUGSNAG_FRAMES_MAX];
  ssize_t count = func(stack, nullptr, nullptr);

  // find JNI references
  auto ArrayList = (*env).FindClass("java/util/ArrayList");
  auto init = (*env).GetMethodID(ArrayList, "<init>", "()V");
  auto add = (*env).GetMethodID(ArrayList, "add", "(Ljava/lang/Object;)Z");

  // make list instance
  auto items = (*env).NewObject(ArrayList, init);

  for (int index = 0; index < count; index++) {
    // using empty string as a sentinel if method is null
    auto method = (*env).NewStringUTF(stack[index].method ?: "");
    (*env).CallBooleanMethod(items, add, method);
  }
  return items;
}

jobject __attribute__((optnone)) unwind_func_three(JNIEnv *env, unwinder func) {
  return unwind_func_four(env, func);
}

jobject __attribute__((optnone)) unwind_func_two(JNIEnv *env, unwinder func) {
  return unwind_func_three(env, func);
}

jobject __attribute__((optnone)) unwind_func_one(JNIEnv *env, unwinder func) {
  return unwind_func_two(env, func);
}

JNIEXPORT jobject JNICALL
Java_com_bugsnag_android_ndk_UnwindTest_unwindForNotify(JNIEnv *env,
                                                        jobject _this) {
  bsg_unwinder_init();
  return unwind_func_one(env, bsg_unwind_concurrent_stack);
}

JNIEXPORT jobject JNICALL
Java_com_bugsnag_android_ndk_UnwindTest_unwindForCrash(JNIEnv *env,
                                                       jobject _this) {
  bsg_unwinder_init();
  return unwind_func_one(env, bsg_unwind_crash_stack);
}

#ifdef __cplusplus
}
#endif
