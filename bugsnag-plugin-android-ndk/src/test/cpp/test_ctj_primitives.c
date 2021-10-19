#include <greatest/greatest.h>
#include <fcntl.h>
#include <unistd.h>
#include <stdbool.h>
#include "crashtime_journal_primitives.h"
#include <ftw.h>
#include <bugsnag_ndk.h>
#include "test_helpers.h"
#include <errno.h>

void bsg_ctjournal_test_reset();
static char journal_filename[PATH_MAX];

static bool init_test() {
    sprintf(journal_filename, "%s/bsg-test.journal.crashtime", test_temporary_folder_path);
    bsg_ctjournal_test_reset();
    return bsg_ctjournal_init(journal_filename);
}

TEST expect_journal_contents(const char* expected_contents, int expected_length) {
    return bsg_expect_file_contents(journal_filename, expected_contents, expected_length);
}

TEST test_ctjournal_init(void) {
    ASSERT(init_test());
    const char expected_contents[] = "{\"*\":{\"type\":\"Bugsnag Journal\",\"version\":1}}\0";
    STOP_ON_FAIL(expect_journal_contents(expected_contents, sizeof(expected_contents)-1));
    PASS();
}

TEST test_ctjournal_clear_value(void) {
    ASSERT(init_test());
    ASSERT(bsg_ctjournal_clear_value("a.-1.b"));
    const char expected_contents[] = "{\"*\":{\"type\":\"Bugsnag Journal\",\"version\":1}}\0"
                                     "{\"a.-1.b\":null}\0";
    STOP_ON_FAIL(expect_journal_contents(expected_contents, sizeof(expected_contents)-1));
    PASS();
}

TEST test_ctjournal_clear_value_escaped(void) {
    ASSERT(init_test());
    ASSERT(bsg_ctjournal_clear_value("a\\\"xx.-1.b"));
    const char expected_contents[] = "{\"*\":{\"type\":\"Bugsnag Journal\",\"version\":1}}\0"
                                     "{\"a\\\\\\\"xx.-1.b\":null}\0";
    STOP_ON_FAIL(expect_journal_contents(expected_contents, sizeof(expected_contents)-1));
    PASS();
}

TEST test_ctjournal_clear_value_escaped_long(void) {
    // Deliberately blow out the internal buffer
    ASSERT(init_test());
    ASSERT(bsg_ctjournal_clear_value("a\\\"xxqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqq.-1.b"));
    const char expected_contents[] = "{\"*\":{\"type\":\"Bugsnag Journal\",\"version\":1}}\0"
                                     "{\"a\\\\\\\"xxqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqq.-1.b\":null}\0";
    STOP_ON_FAIL(expect_journal_contents(expected_contents, sizeof(expected_contents)-1));
    PASS();
}

TEST test_ctjournal_set_double(void) {
    ASSERT(init_test());
    ASSERT(bsg_ctjournal_set_double("a.-1.b", 1.5));
    const char expected_contents[] = "{\"*\":{\"type\":\"Bugsnag Journal\",\"version\":1}}\0"
                                     "{\"a.-1.b\":1.5}\0";
    STOP_ON_FAIL(expect_journal_contents(expected_contents, sizeof(expected_contents)-1));
    PASS();
}

TEST test_ctjournal_set_bool(void) {
    ASSERT(init_test());
    ASSERT(bsg_ctjournal_set_boolean("a.-1.b", true));
    ASSERT(bsg_ctjournal_set_boolean("a.-1.c", false));
    const char expected_contents[] = "{\"*\":{\"type\":\"Bugsnag Journal\",\"version\":1}}\0"
                                     "{\"a.-1.b\":true}\0"
                                     "{\"a.-1.c\":false}\0";
    STOP_ON_FAIL(expect_journal_contents(expected_contents, sizeof(expected_contents)-1));
    PASS();
}

TEST test_ctjournal_set_string(void) {
    ASSERT(init_test());
    ASSERT(bsg_ctjournal_set_string("a.-1.b", "test"));
    const char expected_contents[] = "{\"*\":{\"type\":\"Bugsnag Journal\",\"version\":1}}\0"
                                     "{\"a.-1.b\":\"test\"}\0";
    STOP_ON_FAIL(expect_journal_contents(expected_contents, sizeof(expected_contents)-1));
    PASS();
}

TEST test_ctjournal_set_string_escaped(void) {
            ASSERT(init_test());
            ASSERT(bsg_ctjournal_set_string("a.-1.b", "test\"\\"));
    const char expected_contents[] = "{\"*\":{\"type\":\"Bugsnag Journal\",\"version\":1}}\0"
                                     "{\"a.-1.b\":\"test\\\"\\\\\"}\0";
    STOP_ON_FAIL(expect_journal_contents(expected_contents, sizeof(expected_contents)-1));
            PASS();
}

TEST test_metadata_clear_section(void) {
    ASSERT(init_test());
    ASSERT(bsg_ctjournal_metadata_clear_section("mysection"));
    const char expected_contents[] = "{\"*\":{\"type\":\"Bugsnag Journal\",\"version\":1}}\0"
                                     "{\"events.-1.metaData.mysection\":null}\0";
    STOP_ON_FAIL(expect_journal_contents(expected_contents, sizeof(expected_contents)-1));
    PASS();
}

TEST test_metadata_clear_value(void) {
    ASSERT(init_test());
    ASSERT(bsg_ctjournal_metadata_clear_value("mysection", "myvalue"));
    const char expected_contents[] = "{\"*\":{\"type\":\"Bugsnag Journal\",\"version\":1}}\0"
                                     "{\"events.-1.metaData.mysection.myvalue\":null}\0";
    STOP_ON_FAIL(expect_journal_contents(expected_contents, sizeof(expected_contents)-1));
    PASS();
}

TEST test_metadata_clear_value_escaped(void) {
            ASSERT(init_test());
            ASSERT(bsg_ctjournal_metadata_clear_value("my\\\"section", "my\\\"value"));
    const char expected_contents[] = "{\"*\":{\"type\":\"Bugsnag Journal\",\"version\":1}}\0"
                                     "{\"events.-1.metaData.my\\\\\\\"section.my\\\\\\\"value\":null}\0";
    STOP_ON_FAIL(expect_journal_contents(expected_contents, sizeof(expected_contents)-1));
            PASS();
}

TEST test_metadata_set_boolean(void) {
    ASSERT(init_test());
    ASSERT(bsg_ctjournal_metadata_set_boolean("mysection", "myvalue", false));
    const char expected_contents[] = "{\"*\":{\"type\":\"Bugsnag Journal\",\"version\":1}}\0"
                                     "{\"events.-1.metaData.mysection.myvalue\":false}\0";
    STOP_ON_FAIL(expect_journal_contents(expected_contents, sizeof(expected_contents)-1));
    PASS();
}

TEST test_metadata_set_double(void) {
    ASSERT(init_test());
    ASSERT(bsg_ctjournal_metadata_set_double("mysection", "myvalue", -6.194e+50));
    const char expected_contents[] = "{\"*\":{\"type\":\"Bugsnag Journal\",\"version\":1}}\0"
                                     "{\"events.-1.metaData.mysection.myvalue\":-6.194e+50}\0";
    STOP_ON_FAIL(expect_journal_contents(expected_contents, sizeof(expected_contents)-1));
    PASS();
}

TEST test_metadata_set_string(void) {
    ASSERT(init_test());
    ASSERT(bsg_ctjournal_metadata_set_string("mysection", "myvalue", "fred"));
    const char expected_contents[] = "{\"*\":{\"type\":\"Bugsnag Journal\",\"version\":1}}\0"
                                     "{\"events.-1.metaData.mysection.myvalue\":\"fred\"}\0";
    STOP_ON_FAIL(expect_journal_contents(expected_contents, sizeof(expected_contents)-1));
    PASS();
}

TEST test_metadata_set_string_escaped(void) {
            ASSERT(init_test());
            ASSERT(bsg_ctjournal_metadata_set_string("mysection", "myvalue", "f\\r\"ed"));
    const char expected_contents[] = "{\"*\":{\"type\":\"Bugsnag Journal\",\"version\":1}}\0"
                                     "{\"events.-1.metaData.mysection.myvalue\":\"f\\\\r\\\"ed\"}\0";
    STOP_ON_FAIL(expect_journal_contents(expected_contents, sizeof(expected_contents)-1));
            PASS();
}

TEST test_ctjournal_set_user(void) {
    ASSERT(init_test());
    ASSERT(bsg_ctjournal_set_user("myid", "myemail", "myname"));
    const char expected_contents[] = "{\"*\":{\"type\":\"Bugsnag Journal\",\"version\":1}}\0"
                                     "{\"events.-1.user\":{\"id\":\"myid\",\"email\":\"myemail\",\"name\":\"myname\"}}\0";
    STOP_ON_FAIL(expect_journal_contents(expected_contents, sizeof(expected_contents)-1));
    PASS();
}

TEST test_add_breadcrumb(void) {
    ASSERT(init_test());
    bugsnag_breadcrumb bc = {
        .name =  "myname",
        .timestamp =  "2021-06-02T08:56:06.106Z",
        .type =  BSG_CRUMB_USER,
        .metadata =  {
        .value_count =  4,
        .values =  {
                {
                        .name =  "mybool",
                        .section =  "mysection",
                        .type =  BSG_METADATA_BOOL_VALUE,
                        .bool_value =  true,
                },
                {
                    .name =  "mynull",
                    .section =  "mysection",
                    .type =  BSG_METADATA_NONE_VALUE,
                },
                {
                    .name =  "mynumber",
                    .section =  "mysection",
                    .type =  BSG_METADATA_NUMBER_VALUE,
                    .double_value =  3.91444e-20,
                },
                {
                    .name =  "mystring",
                    .section =  "mysection",
                    .type =  BSG_METADATA_CHAR_VALUE,
                    .char_value =  "a string",
                },
    }
    }
    };
    ASSERT(bsg_ctjournal_add_breadcrumb(&bc));
    const char expected_contents[] = "{\"*\":{\"type\":\"Bugsnag Journal\",\"version\":1}}\0"
                                     "{\"events.-1.breadcrumbs.\":{\"name\":\"myname\",\"timestamp\":\"2021-06-02T08:56:06.106Z\",\"type\":\"user\"}}\0"
                                     "{\"events.-1.breadcrumbs.-1.metaData.mysection.mybool\":true}\0"
                                     "{\"events.-1.breadcrumbs.-1.metaData.mysection.mynull\":null}\0"
                                     "{\"events.-1.breadcrumbs.-1.metaData.mysection.mynumber\":3.91444e-20}\0"
                                     "{\"events.-1.breadcrumbs.-1.metaData.mysection.mystring\":\"a string\"}\0";
    STOP_ON_FAIL(expect_journal_contents(expected_contents, sizeof(expected_contents)-1));
    PASS();
}

TEST test_add_breadcrumb_escaped(void) {
    ASSERT(init_test());
    bugsnag_breadcrumb bc = {
            .name =  "my\\na\"me",
            .timestamp =  "2021-06-02T08:56:06.106Z",
            .type =  BSG_CRUMB_USER,
            .metadata =  {
                    .value_count =  4,
                    .values =  {
                            {
                                    .name =  "my\"b\\ool",
                                    .section =  "my\\s\"ection",
                                    .type =  BSG_METADATA_BOOL_VALUE,
                                    .bool_value =  true,
                            },
                            {
                                    .name =  "mynull",
                                    .section =  "mysection",
                                    .type =  BSG_METADATA_NONE_VALUE,
                            },
                            {
                                    .name =  "mynumber",
                                    .section =  "mysection",
                                    .type =  BSG_METADATA_NUMBER_VALUE,
                                    .double_value =  3.91444e-20,
                            },
                            {
                                    .name =  "mystring",
                                    .section =  "mysection",
                                    .type =  BSG_METADATA_CHAR_VALUE,
                                    .char_value =  "\"a\" str\\ing",
                            },
                    }
            }
    };
    ASSERT(bsg_ctjournal_add_breadcrumb(&bc));
    const char expected_contents[] = "{\"*\":{\"type\":\"Bugsnag Journal\",\"version\":1}}\0"
                                     "{\"events.-1.breadcrumbs.\":{\"name\":\"my\\\\na\\\"me\",\"timestamp\":\"2021-06-02T08:56:06.106Z\",\"type\":\"user\"}}\0"
                                     "{\"events.-1.breadcrumbs.-1.metaData.my\\\\s\\\"ection.my\\\"b\\\\ool\":true}\0"
                                     "{\"events.-1.breadcrumbs.-1.metaData.mysection.mynull\":null}\0"
                                     "{\"events.-1.breadcrumbs.-1.metaData.mysection.mynumber\":3.91444e-20}\0"
                                     "{\"events.-1.breadcrumbs.-1.metaData.mysection.mystring\":\"\\\"a\\\" str\\\\ing\"}\0";
    STOP_ON_FAIL(expect_journal_contents(expected_contents, sizeof(expected_contents)-1));
    PASS();
}

SUITE(suite_ctj_primitives) {
    RUN_TEST(test_ctjournal_init);
    RUN_TEST(test_ctjournal_clear_value);
    RUN_TEST(test_ctjournal_clear_value_escaped);
    RUN_TEST(test_ctjournal_clear_value_escaped_long);
    RUN_TEST(test_ctjournal_set_double);
    RUN_TEST(test_ctjournal_set_bool);
    RUN_TEST(test_ctjournal_set_string);
    RUN_TEST(test_ctjournal_set_string_escaped);
    RUN_TEST(test_metadata_clear_section);
    RUN_TEST(test_metadata_clear_value);
    RUN_TEST(test_metadata_clear_value_escaped);
    RUN_TEST(test_metadata_set_boolean);
    RUN_TEST(test_metadata_set_double);
    RUN_TEST(test_metadata_set_string);
    RUN_TEST(test_metadata_set_string_escaped);
    RUN_TEST(test_ctjournal_set_user);
    RUN_TEST(test_add_breadcrumb);
    RUN_TEST(test_add_breadcrumb_escaped);
}
