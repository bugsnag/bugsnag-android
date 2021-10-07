#include <greatest/greatest.h>
#include "utils/path_builder.h"
#include "test_helpers.h"

TEST test_something(void) {
    bsg_pb_reset();
    ASSERT_STR_EQ("", bsg_pb_path());
    ASSERT_STR_EQ("a", bsg_pb_path());

    PASS();
}

SUITE(suite_path_builder) {
    RUN_TEST(test_something);
}
