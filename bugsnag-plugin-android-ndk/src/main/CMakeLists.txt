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
        jni/internal_metrics.c
        jni/handlers/signal_handler.c
        jni/handlers/cpp_handler.cpp
        jni/utils/crash_info.c
        jni/utils/serializer/buffered_writer.c
        jni/utils/serializer/event_writer.c
        jni/utils/serializer/BSG_KSJSONCodec.c
        jni/utils/serializer/BSG_KSCrashStringConversion.c
        jni/utils/stack_unwinder.cpp
        jni/utils/seqlock.c
        jni/utils/serializer.c
        jni/utils/string.c
        jni/utils/threads.c
        jni/utils/memory.c
        jni/deps/parson/parson.c
)

include_directories(
        jni
        jni/deps
        jni/external/libunwindstack-ndk/include
)

target_include_directories(bugsnag-ndk PRIVATE ${BUGSNAG_DIR}/assets/include)

target_link_libraries(
        # Specifies the target library.
        bugsnag-ndk
        # Links the log library to the target library.
        log
)

# Avoid exporting symbols in release mode to keep internals private
# More symbols are exported in debug mode for the sake of unit testing
set(EXTRA_LINK_FLAGS "-Wl,-z,max-page-size=16384,--version-script=${CMAKE_CURRENT_LIST_DIR}/exported_native_symbols-${CMAKE_BUILD_TYPE}.txt")

set_target_properties(
        bugsnag-ndk
        PROPERTIES
        COMPILE_OPTIONS -Werror -Wall -pedantic
        LINK_FLAGS "${EXTRA_LINK_FLAGS}"
        CXX_STANDARD 17
        CXX_STANDARD_REQUIRED YES
)

add_subdirectory(jni/external/libunwindstack-ndk/cmake)
target_link_libraries(bugsnag-ndk unwindstack)
