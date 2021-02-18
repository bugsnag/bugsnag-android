#include <greatest/greatest.h>
#include <utils/string.h>
#include <stdlib.h>

TEST test_copy_empty_string(void) {
    int length = 10;
    char *src = "";
    char *dst = calloc(sizeof(char), length);
    dst[0] = 'a';
    bsg_strncpy_safe(dst, src, length);
    ASSERT(dst[0] == '\0');
    free(dst);
    PASS();
}

TEST test_copy_literal_string(void) {
    int length = 10;
    char *src = "C h a n g e";
    char *dst = calloc(sizeof(char), length);
    bsg_strncpy_safe(dst, src, length);
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


SUITE(string_utils) {
    RUN_TEST(test_copy_empty_string);
    RUN_TEST(test_copy_literal_string);
}

