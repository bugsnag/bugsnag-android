#include <cstdint>
#include <cstdio>
#include <jni.h>

#include <utils/stack_unwinder.h>

extern "C" {

typedef ssize_t (*unwinder)(bugsnag_stackframe *, siginfo *, void *);

ssize_t bsg_unwind_crash_stack(bugsnag_stackframe stack[BUGSNAG_FRAMES_MAX],
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
  auto HashMap = (*env).FindClass("java/util/HashMap");
  auto Long = (*env).FindClass("java/lang/Long");
  auto listInit = (*env).GetMethodID(ArrayList, "<init>", "()V");
  auto listAdd = (*env).GetMethodID(ArrayList, "add", "(Ljava/lang/Object;)Z");
  auto longInit = (*env).GetMethodID(Long, "<init>", "(J)V");
  auto mapInit = (*env).GetMethodID(HashMap, "<init>", "()V");
  auto mapPut = (*env).GetMethodID(
      HashMap, "put",
      "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;");

  // make list instance
  auto frames = (*env).NewObject(ArrayList, listInit);

  for (int index = 0; index < count; index++) {
    auto frame = (*env).NewObject(HashMap, mapInit);

    // using empty string as a sentinel if method or file is null
    auto file = stack[index].filename[0] ? stack[index].filename : "";
    auto method = stack[index].method[0] ? stack[index].method : "";
    jlong lineNumber = stack[index].line_number;
    jlong symbolAddress = stack[index].symbol_address;
    jlong frameAddress = stack[index].frame_address;
    jlong loadAddress = stack[index].load_address;

    (*env).CallObjectMethod(frame, mapPut, (*env).NewStringUTF("file"),
                            (*env).NewStringUTF(file));
    (*env).CallObjectMethod(frame, mapPut, (*env).NewStringUTF("method"),
                            (*env).NewStringUTF(method));

    (*env).CallObjectMethod(
        frame, mapPut, (*env).NewStringUTF("lineNumber"),
        (*env).NewObject(Long, longInit, lineNumber));
    (*env).CallObjectMethod(
        frame, mapPut, (*env).NewStringUTF("symbolAddress"),
        (*env).NewObject(Long, longInit, symbolAddress));
    (*env).CallObjectMethod(
        frame, mapPut, (*env).NewStringUTF("frameAddress"),
        (*env).NewObject(Long, longInit, frameAddress));
    (*env).CallObjectMethod(
        frame, mapPut, (*env).NewStringUTF("loadAddress"),
        (*env).NewObject(Long, longInit, loadAddress));

    (*env).CallBooleanMethod(frames, listAdd, frame);
  }

  return frames;
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

JNIEXPORT jobject JNICALL
Java_com_bugsnag_android_ndk_UnwindTest_00024BuildInfo_getNativeFunctionInfo(
    JNIEnv *env, jobject _this) {
  // find JNI references
  auto HashMap = (*env).FindClass("java/util/HashMap");
  auto Long = (*env).FindClass("java/lang/Long");
  auto longInit = (*env).GetMethodID(Long, "<init>", "(J)V");
  auto mapInit = (*env).GetMethodID(HashMap, "<init>", "()V");
  auto put = (*env).GetMethodID(
      HashMap, "put",
      "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;");

  // insert each function as a mapping between function name and address
#define add_function_info(map, func)                                           \
  (*env).CallObjectMethod(map, put, (*env).NewStringUTF(#func),                \
                          (*env).NewObject(Long, longInit, (jlong)&func))

  // make list of addresses
  auto items = (*env).NewObject(HashMap, mapInit);
  add_function_info(items, unwind_func_four);
  add_function_info(items, unwind_func_three);
  add_function_info(items, unwind_func_two);
  add_function_info(items, unwind_func_one);
  add_function_info(items,
                    Java_com_bugsnag_android_ndk_UnwindTest_unwindForCrash);
  add_function_info(items,
                    Java_com_bugsnag_android_ndk_UnwindTest_unwindForNotify);

#undef add_function_info

  return items;
}
}
