set(BUGSNAG_VERSION 1.0.1)
add_library( # Specifies the name of the library.
             bugsnag-ndk

             # Sets the library as a shared library.
             SHARED

             # Provides a relative path to your source file(s).
             jni/bugsnag_ndk.c
             jni/bugsnag.c
             jni/metadata.c
             jni/report.c
             jni/handlers/signal_handler.c
             jni/handlers/anr_handler.c
             jni/handlers/cpp_handler.cpp
             jni/utils/crash_info.c
             jni/utils/stack_unwinder.c
             jni/utils/stack_unwinder_libunwindstack.cpp
             jni/utils/stack_unwinder_libcorkscrew.c
             jni/utils/stack_unwinder_libunwind.c
             jni/utils/stack_unwinder_simple.c
             jni/utils/serializer.c
             jni/utils/string.c
             jni/deps/parson/parson.c
             )

include_directories(
             jni/
             jni/deps
             jni/external/libunwind/include
             jni/external/libunwindstack/include
             )

target_include_directories(bugsnag-ndk PRIVATE ${BUGSNAG_DIR}/assets/include)

find_library( # Defines the name of the path variable that stores the
              # location of the NDK library.
              log-lib

              # Specifies the name of the NDK library that
              # CMake needs to locate.
              log )

target_link_libraries( # Specifies the target library.
                     bugsnag-ndk

                     # Links the log library to the target library.
                     ${log-lib})

set_target_properties(bugsnag-ndk
                      PROPERTIES
                      COMPILE_OPTIONS
                      -Werror -Wall -pedantic)

add_subdirectory(jni/external/libunwindstack/cmake)
target_link_libraries(bugsnag-ndk unwindstack)
if(${ANDROID_ABI} STREQUAL "armeabi" OR ${ANDROID_ABI} STREQUAL "armeabi-v7a")
    add_library(libunwind STATIC IMPORTED)
    set_target_properties(libunwind PROPERTIES IMPORTED_LOCATION
        ${ANDROID_NDK}/sources/cxx-stl/llvm-libc++/libs/${ANDROID_ABI}/libunwind.a)
    target_link_libraries(bugsnag-ndk libunwind)
endif()
