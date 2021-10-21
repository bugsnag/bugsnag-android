#include <greatest/greatest.h>
#include <fcntl.h>
#include <unistd.h>
#include <stdbool.h>
#include "crashtime_journal_primitives.h"
#include <ftw.h>
#include <bugsnag_ndk.h>
#include "test_helpers.h"
#include <errno.h>

void bsg_ctj_test_reset();
static char journal_filename[PATH_MAX];

static bool init_test() {
    sprintf(journal_filename, "%s/bsg-test.journal.crashtime", test_temporary_folder_path);
    bsg_ctj_test_reset();
    return bsg_ctj_init(journal_filename);
}

TEST expect_journal_contents(const char* expected_contents, int expected_length) {
    return bsg_expect_file_contents(journal_filename, expected_contents, expected_length);
}

TEST test_ctj_init(void) {
    ASSERT(init_test());
    const char expected_contents[] = "{\"*\":{\"type\":\"Bugsnag Journal\",\"version\":1}}\0";
    STOP_ON_FAIL(expect_journal_contents(expected_contents, sizeof(expected_contents)-1));
    PASS();
}

TEST test_ctj_clear_value(void) {
    ASSERT(init_test());
    ASSERT(bsg_ctj_clear_value("a.-1.b"));
    ASSERT(bsg_ctj_flush());
    const char expected_contents[] = "{\"*\":{\"type\":\"Bugsnag Journal\",\"version\":1}}\0"
                                     "{\"a.-1.b\":null}\0";
    STOP_ON_FAIL(expect_journal_contents(expected_contents, sizeof(expected_contents)-1));
    PASS();
}

TEST test_ctj_clear_value_escaped(void) {
    ASSERT(init_test());
    ASSERT(bsg_ctj_clear_value("a\\\"xx.-1.b"));
            ASSERT(bsg_ctj_flush());
    const char expected_contents[] = "{\"*\":{\"type\":\"Bugsnag Journal\",\"version\":1}}\0"
                                     "{\"a\\\\\\\"xx.-1.b\":null}\0";
    STOP_ON_FAIL(expect_journal_contents(expected_contents, sizeof(expected_contents)-1));
    PASS();
}

TEST test_ctj_clear_value_escaped_long(void) {
    // Deliberately blow out the internal buffer
    ASSERT(init_test());
    ASSERT(bsg_ctj_clear_value(
            "a\\\"xxqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqq.-1.b"));
    ASSERT(bsg_ctj_flush());
    const char expected_contents[] = "{\"*\":{\"type\":\"Bugsnag Journal\",\"version\":1}}\0"
                                     "{\"a\\\\\\\"xxqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqq.-1.b\":null}\0";
    STOP_ON_FAIL(expect_journal_contents(expected_contents, sizeof(expected_contents)-1));
    PASS();
}

TEST test_ctj_set_double(void) {
    ASSERT(init_test());
    ASSERT(bsg_ctj_set_double("a.-1.b", 1.5));
    ASSERT(bsg_ctj_flush());
    const char expected_contents[] = "{\"*\":{\"type\":\"Bugsnag Journal\",\"version\":1}}\0"
                                     "{\"a.-1.b\":1.5}\0";
    STOP_ON_FAIL(expect_journal_contents(expected_contents, sizeof(expected_contents)-1));
    PASS();
}

TEST test_ctj_set_bool(void) {
    ASSERT(init_test());
    ASSERT(bsg_ctj_set_boolean("a.-1.b", true));
    ASSERT(bsg_ctj_set_boolean("a.-1.c", false));
    ASSERT(bsg_ctj_flush());
    const char expected_contents[] = "{\"*\":{\"type\":\"Bugsnag Journal\",\"version\":1}}\0"
                                     "{\"a.-1.b\":true}\0"
                                     "{\"a.-1.c\":false}\0";
    STOP_ON_FAIL(expect_journal_contents(expected_contents, sizeof(expected_contents)-1));
    PASS();
}

TEST test_ctj_set_string(void) {
    ASSERT(init_test());
    ASSERT(bsg_ctj_set_string("a.-1.b", "test"));
    ASSERT(bsg_ctj_flush());
    const char expected_contents[] = "{\"*\":{\"type\":\"Bugsnag Journal\",\"version\":1}}\0"
                                     "{\"a.-1.b\":\"test\"}\0";
    STOP_ON_FAIL(expect_journal_contents(expected_contents, sizeof(expected_contents)-1));
    PASS();
}

TEST test_ctj_set_string_escaped(void) {
    ASSERT(init_test());
    ASSERT(bsg_ctj_set_string("a.-1.b", "test\"\\"));
    ASSERT(bsg_ctj_flush());
    const char expected_contents[] = "{\"*\":{\"type\":\"Bugsnag Journal\",\"version\":1}}\0"
                                     "{\"a.-1.b\":\"test\\\"\\\\\"}\0";
    STOP_ON_FAIL(expect_journal_contents(expected_contents, sizeof(expected_contents)-1));
    PASS();
}

SUITE(suite_ctj_primitives) {
    RUN_TEST(test_ctj_init);
    RUN_TEST(test_ctj_clear_value);
    RUN_TEST(test_ctj_clear_value_escaped);
    RUN_TEST(test_ctj_clear_value_escaped_long);
    RUN_TEST(test_ctj_set_double);
    RUN_TEST(test_ctj_set_bool);
    RUN_TEST(test_ctj_set_string);
    RUN_TEST(test_ctj_set_string_escaped);
}
