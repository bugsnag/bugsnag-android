cmake_minimum_required(VERSION 3.4.1)
project(TEST)
add_subdirectory(src/main)

enable_testing()
add_subdirectory(src/test)

