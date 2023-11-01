cmake_minimum_required(VERSION 3.6)

find_package(bugsnag-plugin-android-ndk REQUIRED CONFIG)

add_library(entrypoint SHARED
        src/main/cpp/entrypoint.cpp)

target_include_directories(entrypoint PRIVATE ${BUGSNAG_INCLUDE_DIR})
target_link_libraries(entrypoint bugsnag-plugin-android-ndk::bugsnag-ndk)
