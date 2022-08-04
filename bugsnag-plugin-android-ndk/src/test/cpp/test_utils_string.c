#include <greatest/greatest.h>
#include <utils/string.h>
#include <stdlib.h>

TEST test_copy_empty_string(void) {
    char *src = "";
    int dst_len = 10;
    char *dst = calloc(sizeof(char), dst_len);
    bsg_strncpy(dst, src, dst_len);
    ASSERT(dst[0] == '\0');
    free(dst);
    PASS();
}

TEST test_copy_null_string(void) {
    int dst_len = 10;
    char *dst = calloc(sizeof(char), dst_len);
    strcpy(dst, "C h a n g e");
    bsg_strncpy(dst, NULL, dst_len);
    ASSERT(dst[0] == '\0');
    free(dst);
    PASS();
}

TEST test_copy_literal_string(void) {
    char *src = "C h a n g e";
    int dst_len = 10;
    char *dst = calloc(sizeof(char), dst_len);
    bsg_strncpy(dst, src, dst_len);
    ASSERT(dst[0] == 'C');
    ASSERT(dst[1] == ' ');
    ASSERT(dst[2] == 'h');
    ASSERT(dst[3] == ' ');
    ASSERT(dst[4] == 'a');
    ASSERT(dst[5] == ' ');
    ASSERT(dst[6] == 'n');
    ASSERT(dst[7] == ' ');
    ASSERT(dst[8] == 'g');
    ASSERT(dst[9] == '\0');
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

TEST hex_encode(void) {
    char out_buffer[16];
    char *bytes = "bytes";
    bsg_hex_encode(out_buffer, bytes, strlen(bytes), sizeof(out_buffer));
    ASSERT_STR_EQ("6279746573", out_buffer);
    PASS();
}

TEST hex_encode_zero(void) {
    char out_buffer[256];
    out_buffer[0] = '1';

    bsg_hex_encode(out_buffer, NULL, 0, sizeof(out_buffer));
    ASSERT_EQ(0, *out_buffer);
    PASS();
}

TEST hex_encode_overflow(void) {
    char out_buffer[7];
    char *bytes = "bytes";
    bsg_hex_encode(out_buffer, bytes, strlen(bytes), sizeof(out_buffer));
    // the output must still be zero-terminated
    ASSERT_STR_EQ("627974", out_buffer);
    PASS();
}

TEST hex_encode_exact_length(void) {
    char out_buffer[10];
    char *bytes = "bytes";
    bsg_hex_encode(out_buffer, bytes, strlen(bytes), sizeof(out_buffer));
    // we expect the entire last byte of *input* to be dropped
    ASSERT_STR_EQ("62797465", out_buffer);
    PASS();
}

SUITE(suite_string_utils) {
    RUN_TEST(test_copy_empty_string);
    RUN_TEST(test_copy_null_string);
    RUN_TEST(test_copy_literal_string);
    RUN_TEST(length_empty_string);
    RUN_TEST(length_literal_string);
    RUN_TEST(length_null_string);
    RUN_TEST(hex_encode);
    RUN_TEST(hex_encode_zero);
    RUN_TEST(hex_encode_overflow);
    RUN_TEST(hex_encode_exact_length);
}

