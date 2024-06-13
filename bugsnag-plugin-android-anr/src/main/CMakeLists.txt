set(BUGSNAG_VERSION 1.0.1)
add_library( # Specifies the name of the library.
        bugsnag-plugin-android-anr
        # Sets the library as a shared library.
        SHARED
        # Provides a relative path to your source file(s).
        jni/anr_google.c
        jni/anr_handler.c
        jni/bugsnag_anr.c
        jni/utils/string.c
)

include_directories(jni)

set(EXTRA_LINK_FLAGS "-Wl,-z,max-page-size=16384")

target_link_libraries( # Specifies the target library.
        bugsnag-plugin-android-anr
        # Links the log library to the target library.
        log)

set_target_properties(
        bugsnag-plugin-android-anr
        PROPERTIES
        COMPILE_OPTIONS -Werror -Wall -pedantic
        LINK_FLAGS "${EXTRA_LINK_FLAGS}"
)
