set(BUGSNAG_VERSION 1.0.1)
add_library( # Specifies the name of the library.
             bugsnag-ndk

             # Sets the library as a shared library.
             SHARED

             # Provides a relative path to your source file(s).
    jni/bugsnag_ndk.c
    jni/bugsnag.c
    jni/metadata.c
    jni/safejni.c
    jni/jni_cache.c
    jni/event.c
    jni/featureflags.c
    jni/handlers/signal_handler.c
    jni/handlers/cpp_handler.cpp
    jni/utils/crash_info.c
    jni/utils/serializer/buffered_writer.c
    jni/utils/serializer/event_reader.c
    jni/utils/serializer/event_writer.c
    jni/utils/serializer/json_writer.c
    jni/utils/stack_unwinder.cpp
    jni/utils/stack_unwinder_libcorkscrew.c
    jni/utils/stack_unwinder_libunwind.c
    jni/utils/stack_unwinder_simple.c
    jni/utils/serializer.c
    jni/utils/string.c
    jni/utils/threads.c
    jni/deps/parson/parson.c
             )

include_directories(
    jni
    jni/deps
    jni/external/libunwind/include
    jni/external/libunwindstack-ndk/include
             )

target_include_directories(bugsnag-ndk PRIVATE ${BUGSNAG_DIR}/assets/include)

target_link_libraries( # Specifies the target library.
                     bugsnag-ndk
                     # Links the log library to the target library.
                     log)

set_target_properties(bugsnag-ndk
                      PROPERTIES
                      COMPILE_OPTIONS -Werror -Wall -pedantic
                      CXX_STANDARD 17
                      CXX_STANDARD_REQUIRED YES)

add_subdirectory(jni/external/libunwindstack-ndk/cmake)
target_link_libraries(bugsnag-ndk unwindstack)
