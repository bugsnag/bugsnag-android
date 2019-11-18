cmake_minimum_required(VERSION 3.4.1)

add_library(entrypoint SHARED
             src/main/cpp/entrypoint.cpp
             src/main/cpp/bugsnags.cpp)

#FIXME determine how best to link against bugsnag-ndk library in mazerunner module

set(BUGSNAG_LIB_DIR
    ${CMAKE_SOURCE_DIR}/build/intermediates/bugsnag-libs)
set(BUGSNAG_INCLUDE_DIR ${BUGSNAG_LIB_DIR}/assets/include)
target_include_directories(entrypoint PRIVATE ${BUGSNAG_INCLUDE_DIR})

add_library(lib_monochrome SHARED IMPORTED)
set_target_properties(lib_monochrome PROPERTIES IMPORTED_LOCATION
                      ${CMAKE_SOURCE_DIR}/libs/${ANDROID_ABI}/libmonochrome.so)
target_link_libraries(entrypoint lib_monochrome)

