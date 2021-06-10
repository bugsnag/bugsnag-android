cmake_minimum_required(VERSION 3.4.1)
set(BUGSNAG_VERSION 1.0.1)
add_library( # Specifies the name of the library.
             bugsnag-root-detection
             # Sets the library as a shared library.
             SHARED
             # Provides a relative path to your source file(s).
    jni/root_detection.c
    )

include_directories(jni)

set_target_properties(bugsnag-root-detection
                      PROPERTIES
                      COMPILE_OPTIONS
                      -Werror -Wall -pedantic)
