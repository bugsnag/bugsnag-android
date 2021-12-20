#include <greatest/greatest.h>
#include <utils/string.h>
#include <stdlib.h>

TEST test_copy_empty_string(void) {
    char *src = "";
    char *dst = calloc(sizeof(char), 10);
    bsg_strcpy(dst, src);
    ASSERT(dst[0] == '\0');
    free(dst);
    PASS();
}

TEST test_copy_literal_string(void) {
    char *src = "C h a n g e";
    char *dst = calloc(sizeof(char), 10);
    bsg_strcpy(dst, src);
    ASSERT(dst[0] == 'C');
    ASSERT(dst[1] == ' ');
    ASSERT(dst[2] == 'h');
    ASSERT(dst[3] == ' ');
    ASSERT(dst[4] == 'a');
    ASSERT(dst[5] == ' ');
    ASSERT(dst[6] == 'n');
    ASSERT(dst[7] == ' ');
    ASSERT(dst[8] == 'g');
    ASSERT(dst[9] == ' ');
    ASSERT(dst[10] == 'e');
    ASSERT(dst[11] == '\0');
    free(dst);
    PASS();
}

TEST length_literal_string(void) {
    ASSERT_EQ(11, bsg_strlen("C h a n g e"));
    PASS();
}

TEST length_empty_string(void) {
    ASSERT_EQ(0, bsg_strlen(""));
    PASS();
}

TEST length_null_string(void) {
    ASSERT_EQ(0, bsg_strlen(NULL));
    PASS();
}

SUITE(suite_string_utils) {
    RUN_TEST(test_copy_empty_string);
    RUN_TEST(test_copy_literal_string);
    RUN_TEST(length_empty_string);
    RUN_TEST(length_literal_string);
    RUN_TEST(length_null_string);
}

