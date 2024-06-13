cmake_minimum_required(VERSION 3.6)

find_package(bugsnag-plugin-android-ndk REQUIRED CONFIG)

add_library(entrypoint SHARED
        src/main/cpp/entrypoint.cpp)

set(EXTRA_LINK_FLAGS "-Wl,-z,max-page-size=16384")

target_include_directories(entrypoint PRIVATE ${BUGSNAG_INCLUDE_DIR})
target_link_libraries(entrypoint bugsnag-plugin-android-ndk::bugsnag-ndk)

set_target_properties(
        entrypoint
        PROPERTIES
        COMPILE_OPTIONS -Werror -Wall -pedantic
        LINK_FLAGS "${EXTRA_LINK_FLAGS}"
)
