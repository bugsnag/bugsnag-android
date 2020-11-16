cmake_minimum_required(VERSION 3.4.1)

add_library(cxx-scenarios-bugsnag SHARED
        src/main/cpp/cxx-scenarios-bugsnag.cpp)

add_library(lib_bugsnag SHARED IMPORTED)
set(BUGSNAG_LIB_DIR
    ${CMAKE_SOURCE_DIR}/build/intermediates/bugsnag-libs)
set(BUGSNAG_INCLUDE_DIR ${BUGSNAG_LIB_DIR}/assets/include)
set_target_properties(lib_bugsnag PROPERTIES IMPORTED_LOCATION
                      ${BUGSNAG_LIB_DIR}/jni/${ANDROID_ABI}/libbugsnag-ndk.so)
target_include_directories(cxx-scenarios-bugsnag PRIVATE ${BUGSNAG_INCLUDE_DIR})
target_link_libraries(cxx-scenarios-bugsnag lib_bugsnag)
