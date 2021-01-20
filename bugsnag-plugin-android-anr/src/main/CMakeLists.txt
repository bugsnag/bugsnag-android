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

find_library( # Defines the name of the path variable that stores the
              # location of the NDK library.
              log-lib
              # Specifies the name of the NDK library that
              # CMake needs to locate.
              log )

target_link_libraries( # Specifies the target library.
                     bugsnag-plugin-android-anr
                     # Links the log library to the target library.
                     ${log-lib})

set_target_properties(bugsnag-plugin-android-anr
                      PROPERTIES
                      COMPILE_OPTIONS
                      -Werror -Wall -pedantic)
