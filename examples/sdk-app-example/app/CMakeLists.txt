cmake_minimum_required(VERSION 3.4.1)

add_library(entrypoint SHARED
             src/main/cpp/entrypoint.cpp)

add_library(lib_bugsnag SHARED IMPORTED)
set(BUGSNAG_LIB_DIR
    ${CMAKE_SOURCE_DIR}/build/intermediates/bugsnag-libs)
set(BUGSNAG_INCLUDE_DIR ${BUGSNAG_LIB_DIR}/assets/include)
set(EXTRA_LINK_FLAGS "-Wl,-z,max-page-size=16384")
set_target_properties(lib_bugsnag PROPERTIES IMPORTED_LOCATION
                      ${BUGSNAG_LIB_DIR}/jni/${ANDROID_ABI}/libbugsnag-ndk.so
                      LINK_FLAGS "${EXTRA_LINK_FLAGS}"
)
target_include_directories(entrypoint PRIVATE ${BUGSNAG_INCLUDE_DIR})
target_link_libraries(entrypoint lib_bugsnag)
