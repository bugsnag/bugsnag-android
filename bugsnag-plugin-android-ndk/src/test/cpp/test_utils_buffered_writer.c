#include <greatest/greatest.h>
#include "utils/buffered_writer.h"
#include "test_helpers.h"

static char writer_filename[PATH_MAX];

static void init_test() {
    sprintf(writer_filename, "%s/writer.bin", test_temporary_folder_path);
}

TEST test_open_close(void) {
    init_test();
    bsg_buffered_writer *writer = bsg_buffered_writer_open(100, writer_filename);
    ASSERT(writer != NULL);
    ASSERT(writer->dispose(writer));
    STOP_ON_FAIL(bsg_expect_file_contents(writer_filename, "", 0));
    PASS();
}

TEST test_write_1_byte(void) {
    init_test();
    bsg_buffered_writer *writer = bsg_buffered_writer_open(100, writer_filename);
    ASSERT(writer != NULL);
    ASSERT(writer->write(writer, "1", 1));
    ASSERT(writer->dispose(writer));
    STOP_ON_FAIL(bsg_expect_file_contents(writer_filename, "1", 1));
    PASS();
}

TEST test_write_multiple(void) {
    init_test();
    bsg_buffered_writer *writer = bsg_buffered_writer_open(10, writer_filename);
    ASSERT(writer != NULL);
    ASSERT(writer->write(writer, "1", 1));
    ASSERT(writer->write(writer, "\x13\xff\x00.abc", 7));
    ASSERT(writer->write(writer, "\nblah\n", 6));
    ASSERT(writer->dispose(writer));
    STOP_ON_FAIL(bsg_expect_file_contents(writer_filename, "1\x13\xff\x00.abc\nblah\n", 14));
    PASS();
}

TEST test_write_same_size_as_buffer(void) {
    init_test();
    bsg_buffered_writer *writer = bsg_buffered_writer_open(10, writer_filename);
    ASSERT(writer != NULL);
    ASSERT(writer->write(writer, "1234567890", 10));
    ASSERT(writer->dispose(writer));
    STOP_ON_FAIL(bsg_expect_file_contents(writer_filename, "1234567890", 10));
    PASS();
}

TEST test_write_too_big(void) {
    init_test();
    bsg_buffered_writer *writer = bsg_buffered_writer_open(10, writer_filename);
    ASSERT(writer != NULL);
    ASSERT_FALSE(writer->write(writer, "12345678901", 11));
    PASS();
}

SUITE(suite_buffered_writer) {
    RUN_TEST(test_open_close);
    RUN_TEST(test_write_1_byte);
    RUN_TEST(test_write_multiple);
    RUN_TEST(test_write_same_size_as_buffer);
    RUN_TEST(test_write_too_big);
}
