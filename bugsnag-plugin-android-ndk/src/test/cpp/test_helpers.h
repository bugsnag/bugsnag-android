#include <stdbool.h>
#include <greatest/greatest.h>

/**
 * Temporary folder path for tests to play in. This must be set from the JNI entry point.
 */
extern const char *test_temporary_folder_path;

/**
 * Helper macro to pass along FAIL() directives and assertion failures from inside a sub-function.
 * Simply have the sub-function return type int (or TEST if internal), then wrap the call:
 *
 *     STOP_ON_FAIL(my_sub_function(myargs));
 */
#define STOP_ON_FAIL(...) do { \
    int res = __VA_ARGS__; \
    if (res != GREATEST_TEST_RES_PASS) { \
        return res; \
    } \
} \
while(0)


/**
 * Check if two buffers match.
 *
 * @param mem1 The first buffer
 * @param length1 The length of the first buffer
 * @param mem2 The second buffer
 * @param length2 The length of the second buffer
 * @return True if the buffers match
 */
bool bsg_do_mem_contents_match(const char* mem1, int length1, const char* mem2, int length2);

/**
 * Convert a buffer to printable characters.
 *
 * Converts any non-printable or control characters besides CR,LF,TAB to their hex equivalents in
 * the format "\xff"
 *
 * @param data The data to convert
 * @param length The length of the data
 * @return A printable version which must be freed.
 */
const char* bsg_as_printable(const char* data, int length);

/**
 * Verify that a file contains the expected contents.
 * This will print out the differences to BUGSNAG_LOG() before returning a failure if the contents don't match.
 *
 * The easiest way to use this function is:
 *
 * STOP_ON_FAIL(bsg_expect_file_contents(file_path, expected_contents, length));
 *
 * @param path The path of the file to check.
 * @param expected_contents The expected contents.
 * @param expected_length Length of the expected contents.
 * @return A test result.
 */
int bsg_expect_file_contents(const char *path, const char *expected_contents, int expected_length);
