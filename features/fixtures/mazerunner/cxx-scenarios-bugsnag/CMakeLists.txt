cmake_minimum_required(VERSION 3.4.1)
project(cxx-scenarios-bugsnag)

find_package(bugsnag-plugin-android-ndk REQUIRED CONFIG)

add_library(cxx-scenarios-bugsnag SHARED
        src/main/cpp/cxx-scenarios-bugsnag.cpp
        src/main/cpp/CXXExternalStackElementScenario.cpp
        src/main/cpp/CXXExceptionSmokeScenario.cpp)

set_target_properties(cxx-scenarios-bugsnag PROPERTIES
                      CXX_STANDARD 11
                      CXX_STANDARD_REQUIRED YES)

target_include_directories(cxx-scenarios-bugsnag PRIVATE ${BUGSNAG_INCLUDE_DIR})
target_link_libraries(cxx-scenarios-bugsnag bugsnag-plugin-android-ndk::bugsnag-ndk)
