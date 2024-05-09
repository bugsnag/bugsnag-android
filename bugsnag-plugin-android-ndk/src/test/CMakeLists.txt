include_directories(
    ../main/jni
    ../main/jni/deps
    cpp
    cpp/deps
)
add_library(bugsnag-ndk-test SHARED
    cpp/main.c
    cpp/test_utils_string.c
    cpp/test_utils_serialize.c
    cpp/test_serializer.c
    cpp/test_breadcrumbs.c
    cpp/test_bsg_event.c
    cpp/test_featureflags.c
    cpp/test_bsg_event.c
    cpp/UnwindTest.cpp
)
target_link_libraries(bugsnag-ndk-test bugsnag-ndk)
