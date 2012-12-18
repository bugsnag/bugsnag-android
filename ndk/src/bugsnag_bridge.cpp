#include <jni.h>

extern "C" {
    JavaVM* java_vm;

    jint JNI_OnLoad(JavaVM* vm, void* reserved) {
        java_vm = vm;

        // minimum JNI version
        return JNI_VERSION_1_6;
    }

    JNIEnv *getEnv() {
        JNIEnv *jni_env = 0;
        java_vm->AttachCurrentThread(&jni_env, 0);

        return jni_env;
    }

    void SetUserId(char *userId) {
        JNIEnv *jni_env = getEnv();
        
        jclass cls_JavaClass = jni_env->FindClass("com/bugsnag/android/BugsnagUnity");
        jmethodID methodId   = jni_env->GetStaticMethodID(cls_JavaClass, "setUserId", "(Ljava/lang/String;)V");
        
        jni_env->CallStaticVoidMethod(cls_JavaClass, methodId, jni_env->NewStringUTF(userId));
    }
    
    void SetContext(char *context) {
        JNIEnv *jni_env = getEnv();
        
        jclass cls_JavaClass = jni_env->FindClass("com/bugsnag/android/BugsnagUnity");
        jmethodID methodId   = jni_env->GetStaticMethodID(cls_JavaClass, "setContext", "(Ljava/lang/String;)V");
        
        jni_env->CallStaticVoidMethod(cls_JavaClass, methodId, jni_env->NewStringUTF(context));
    }
    
    void SetReleaseStage(char *releaseStage) {
        JNIEnv *jni_env = getEnv();
        
        jclass cls_JavaClass = jni_env->FindClass("com/bugsnag/android/BugsnagUnity");
        jmethodID methodId   = jni_env->GetStaticMethodID(cls_JavaClass, "setReleaseStage", "(Ljava/lang/String;)V");
        
        jni_env->CallStaticVoidMethod(cls_JavaClass, methodId, jni_env->NewStringUTF(releaseStage));
    }
    
    void SetUseSSL(int useSSL) {
        JNIEnv *jni_env = getEnv();
        
        jclass cls_JavaClass = jni_env->FindClass("com/bugsnag/android/BugsnagUnity");
        jmethodID methodId   = jni_env->GetStaticMethodID(cls_JavaClass, "setUseSSL", "(Z)V");
        
        jni_env->CallStaticVoidMethod(cls_JavaClass, methodId, (jboolean)useSSL);
    }
    
    void SetAutoNotify(int autoNotify) {
        JNIEnv *jni_env = getEnv();
        
        jclass cls_JavaClass = jni_env->FindClass("com/bugsnag/android/BugsnagUnity");
        jmethodID methodId   = jni_env->GetStaticMethodID(cls_JavaClass, "setAutoNotify", "(Z)V");
        
        jni_env->CallStaticVoidMethod(cls_JavaClass, methodId, (jboolean)autoNotify);
    }
    
    void Notify(char *errorClass, char *errorMessage, char *stackTrace) {
        JNIEnv *jni_env = getEnv();
        
        jclass cls_JavaClass = jni_env->FindClass("com/bugsnag/android/BugsnagUnity");
        jmethodID methodId   = jni_env->GetStaticMethodID(cls_JavaClass, "notify", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V");
        
        jni_env->CallStaticVoidMethod(cls_JavaClass, methodId, jni_env->NewStringUTF(errorClass), jni_env->NewStringUTF(errorMessage), jni_env->NewStringUTF(stackTrace));
    }
    
    void AddToTab(char *tabName, char *attributeName, char *attributeValue) {
        JNIEnv *jni_env = getEnv();
        
        jclass cls_JavaClass = jni_env->FindClass("com/bugsnag/android/BugsnagUnity");
        jmethodID methodId   = jni_env->GetStaticMethodID(cls_JavaClass, "addToTab", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V");
        
        jni_env->CallStaticVoidMethod(cls_JavaClass, methodId, jni_env->NewStringUTF(tabName), jni_env->NewStringUTF(attributeName), jni_env->NewStringUTF(attributeValue));
    }
    
    void ClearTab(char *tabName) {
        JNIEnv *jni_env = getEnv();
        
        jclass cls_JavaClass = jni_env->FindClass("com/bugsnag/android/BugsnagUnity");
        jmethodID methodId   = jni_env->GetStaticMethodID(cls_JavaClass, "clearTab", "(Ljava/lang/String;)V");
        
        jni_env->CallStaticVoidMethod(cls_JavaClass, methodId, jni_env->NewStringUTF(tabName));
    }
    
    void Register(char *apiKey) {
        JNIEnv *jni_env = getEnv();
        
        jclass cls_JavaClass = jni_env->FindClass("com/bugsnag/android/BugsnagUnity");
        jmethodID methodId   = jni_env->GetStaticMethodID(cls_JavaClass, "register", "(Ljava/lang/String;)V");
        
        jni_env->CallStaticVoidMethod(cls_JavaClass, methodId, jni_env->NewStringUTF(apiKey));
    }
}