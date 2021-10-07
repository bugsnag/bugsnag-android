include_directories(
    ../main/jni
    ../main/jni/deps
    cpp
    cpp/deps
)
add_library(bugsnag-ndk-test SHARED
    cpp/main.c
    cpp/test_utils_buffered_writer.c
    cpp/test_utils_number_to_string.c
    cpp/test_utils_path_builder.c
    cpp/test_utils_string.c
    cpp/test_utils_serialize.c
    cpp/test_serializer.c
    cpp/test_breadcrumbs.c
    cpp/test_bsg_event.c
    cpp/test_ctjournal.c
    cpp/test_journal_save_event.c
    cpp/test_helpers.c
)
target_link_libraries(bugsnag-ndk-test bugsnag-ndk)
