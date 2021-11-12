#include <greatest/greatest.h>
#include "utils/path_builder.h"
#include "test_helpers.h"

#define PATH_SIZE 500
#define PADDED_PATH_SIZE 600
#define MAX_SUBPATHS 100

TEST test_reset(void) {
    bsg_pb_reset();
    ASSERT_STR_EQ("", bsg_pb_path());
    bsg_pb_stack_list_index(1);
    bsg_pb_stack_map_key("abcd");
    ASSERT_STR_EQ("1.abcd", bsg_pb_path());
    bsg_pb_reset();
    ASSERT_STR_EQ("", bsg_pb_path());
    PASS();
}

TEST test_general_use(void) {
    bsg_pb_reset();
    ASSERT_STR_EQ("", bsg_pb_path());
    bsg_pb_stack_map_key("foo");
    ASSERT_STR_EQ("foo", bsg_pb_path());
    bsg_pb_stack_list_index(-1);
    ASSERT_STR_EQ("foo.-1", bsg_pb_path());
    bsg_pb_stack_map_key("bar");
    ASSERT_STR_EQ("foo.-1.bar", bsg_pb_path());
    bsg_pb_stack_list_index(500);
    ASSERT_STR_EQ("foo.-1.bar.500", bsg_pb_path());
    bsg_pb_unstack();
    ASSERT_STR_EQ("foo.-1.bar", bsg_pb_path());
    bsg_pb_unstack();
    ASSERT_STR_EQ("foo.-1", bsg_pb_path());
    bsg_pb_stack_list_index(2);
    ASSERT_STR_EQ("foo.-1.2", bsg_pb_path());
    bsg_pb_unstack();
    ASSERT_STR_EQ("foo.-1", bsg_pb_path());
    bsg_pb_unstack();
    ASSERT_STR_EQ("foo", bsg_pb_path());
    bsg_pb_stack_map_key("bar");
    ASSERT_STR_EQ("foo.bar", bsg_pb_path());
    bsg_pb_unstack();
    ASSERT_STR_EQ("foo", bsg_pb_path());
    bsg_pb_unstack();
    ASSERT_STR_EQ("", bsg_pb_path());
    bsg_pb_unstack();
    ASSERT_STR_EQ("", bsg_pb_path());
    bsg_pb_stack_map_key("bar");
    ASSERT_STR_EQ("bar", bsg_pb_path());
    bsg_pb_stack_raw_key("x+");
    ASSERT_STR_EQ("bar.x+", bsg_pb_path());
    bsg_pb_unstack();
    bsg_pb_unstack();
    ASSERT_STR_EQ("", bsg_pb_path());
    PASS();
}

TEST test_auto_escape(void) {
    bsg_pb_reset();
    ASSERT_STR_EQ("", bsg_pb_path());
    bsg_pb_stack_map_key("foo");
    ASSERT_STR_EQ("foo", bsg_pb_path());
    bsg_pb_stack_map_key("0");
    ASSERT_STR_EQ("foo.\\0", bsg_pb_path());
}

TEST test_unstack_too_far(void) {
    bsg_pb_reset();
    ASSERT_STR_EQ("", bsg_pb_path());
    bsg_pb_unstack();
    ASSERT_STR_EQ("", bsg_pb_path());
    bsg_pb_unstack();
    ASSERT_STR_EQ("", bsg_pb_path());

    PASS();
}

TEST test_stack_too_far(void) {
    bsg_pb_reset();
    ASSERT_STR_EQ("", bsg_pb_path());
    for (int i = 0; i < MAX_SUBPATHS; i++) {
        bsg_pb_stack_list_index(1);
    }
    const char *checkpoint = strdup(bsg_pb_path());
    for (int i = 0; i < 10; i++) {
        bsg_pb_stack_list_index(1);
    }
    ASSERT_STR_EQ(checkpoint, bsg_pb_path());

    PASS();
}

TEST test_path_too_long(void) {
    bsg_pb_reset();
    ASSERT_STR_EQ("", bsg_pb_path());
    for (int i = 0; i < MAX_SUBPATHS - 10; i++) {
        bsg_pb_stack_map_key("abcdefghijklmnopqrstuvwxyz");
    }
    ASSERT(strlen(bsg_pb_path()) < PADDED_PATH_SIZE);

    bsg_pb_reset();
    ASSERT_STR_EQ("", bsg_pb_path());
    for (int i = 0; i < MAX_SUBPATHS - 10; i++) {
        bsg_pb_stack_list_index(9876543210);
    }
    ASSERT(strlen(bsg_pb_path()) < PADDED_PATH_SIZE);

    PASS();
}


SUITE(suite_path_builder) {
    RUN_TEST(test_reset);
    RUN_TEST(test_general_use);
    RUN_TEST(test_auto_escape);
    RUN_TEST(test_unstack_too_far);
    RUN_TEST(test_stack_too_far);
    RUN_TEST(test_path_too_long);
}
