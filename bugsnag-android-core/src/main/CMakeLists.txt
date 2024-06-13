set(BUGSNAG_VERSION 1.0.1)
add_library( # Specifies the name of the library.
        bugsnag-root-detection
        # Sets the library as a shared library.
        SHARED
        # Provides a relative path to your source file(s).
        jni/root_detection.c
)

include_directories(jni)

set(EXTRA_LINK_FLAGS "-Wl,-z,max-page-size=16384")

set_target_properties(
        bugsnag-root-detection
        PROPERTIES
        COMPILE_OPTIONS -Werror -Wall -pedantic
        LINK_FLAGS "${EXTRA_LINK_FLAGS}"
)
