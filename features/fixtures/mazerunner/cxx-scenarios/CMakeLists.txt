cmake_minimum_required(VERSION 3.4.1)
project(cxx-scenarios)

add_library(cxx-scenarios SHARED
        src/main/cpp/cxx-scenarios.cpp
        src/main/cpp/CXXAbortScenario.cpp
        src/main/cpp/CXXCallNullFunctionPointerScenario.cpp
        src/main/cpp/CXXDereferenceNullScenario.cpp
        src/main/cpp/CXXImproperTypecastScenario.cpp
        src/main/cpp/CXXInvalidRethrow.cpp
        src/main/cpp/CXXStackoverflowScenario.cpp
        src/main/cpp/CXXThrowFromNoexcept.cpp
        src/main/cpp/CXXTrapScenario.cpp
        src/main/cpp/CXXWriteReadOnlyMemoryScenario.cpp
        src/main/cpp/bugsnag-java-scenarios.cpp)

set_target_properties(cxx-scenarios PROPERTIES
        CXX_STANDARD 11
        CXX_STANDARD_REQUIRED YES)

target_link_libraries(cxx-scenarios)

