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
             jni/utils/stack_unwinder.c
             jni/utils/stack_unwinder_libcorkscrew.c
             jni/utils/stack_unwinder_libunwind.c
             jni/utils/serializer.c
             jni/utils/string.c
             jni/deps/parson/parson.c
             )

include_directories(
             jni/
             jni/deps
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

