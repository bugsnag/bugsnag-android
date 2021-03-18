set(BUGSNAG_VERSION 1.0.1)
add_library( # Specifies the name of the library.
             bugsnag-core
             # Sets the library as a shared library.
             SHARED
             # Provides a relative path to your source file(s).
    jni/root_detection.c
    )

include_directories(jni)

set_target_properties(bugsnag-core
                      PROPERTIES
                      COMPILE_OPTIONS
                      -Werror -Wall -pedantic)
