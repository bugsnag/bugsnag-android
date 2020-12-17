cmake_minimum_required(VERSION 3.4.1)

add_library(cxx-scenarios SHARED
        src/main/cpp/cxx-scenarios.cpp
        src/main/cpp/bugsnag-java-scenarios.cpp)

add_library(lib_monochrome SHARED IMPORTED)
set_target_properties(lib_monochrome PROPERTIES IMPORTED_LOCATION
                      ${CMAKE_SOURCE_DIR}/libs/${ANDROID_ABI}/libmonochrome.so)
target_link_libraries(cxx-scenarios lib_monochrome)

