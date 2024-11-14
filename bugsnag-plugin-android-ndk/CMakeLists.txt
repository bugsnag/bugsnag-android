cmake_minimum_required(VERSION 3.19)
project(bugsnag-plugin-android-ndk)
project(TEST)
add_subdirectory(src/main)

if(${CMAKE_BUILD_TYPE} STREQUAL Debug)
  enable_testing()
  add_subdirectory(src/test)
endif()
