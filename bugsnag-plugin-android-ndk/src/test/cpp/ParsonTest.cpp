#include <jni.h>
#include <malloc.h>
#include <parson/parson.h>

#ifdef __cplusplus
extern "C" {
#endif

JNIEXPORT jstring JNICALL
Java_com_bugsnag_android_ndk_ParsonTest_longToJsonArray(JNIEnv *env,
                                                        jobject _this,
                                                        jlong value) {
  JSON_Value *ary_value = json_value_init_array();
  JSON_Array *ary = json_value_get_array(ary_value);
  json_array_append_integer(ary, value);
  auto serialized = json_serialize_to_string(ary_value);
  auto result = (*env).NewStringUTF(serialized);

  json_value_free(ary_value);
  free(serialized);

  return result;
}

#ifdef __cplusplus
}
#endif
