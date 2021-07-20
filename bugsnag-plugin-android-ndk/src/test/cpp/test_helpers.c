#include <errno.h>
#include <bugsnag_ndk.h>
#include "test_helpers.h"

const char *test_temporary_folder_path;

bool bsg_do_mem_contents_match(const char* mem1, int length1, const char* mem2, int length2) {
    if (length1 != length2) {
        return false;
    }

    for (int i = 0; i < length1; i++) {
        if (mem1[i] != mem2[i]) {
            return false;
        }
    }

    return true;
}

static char hex_char(unsigned int ch) {
    if (ch <= 9) {
        return '0' + ch;
    } else {
        return 'a' + ch - 10;
    }
}

const char* bsg_as_printable(const char* data, int length) {
    char* new_contents = calloc(1, length*3+1);
    int i_new = 0;
    for(int i_old = 0; i_old < length; i_old++) {
        unsigned char b = (unsigned char)data[i_old];
        if((b >= ' ' && b <= '~') || b == '\r' || b == '\n' || b == '\t') {
            new_contents[i_new++] = b;
        } else {
            new_contents[i_new++] = '\\';
            new_contents[i_new++] = 'x';
            new_contents[i_new++] = hex_char(b>>4);
            new_contents[i_new++] = hex_char(b&15);
        }
    }
    new_contents[i_new] = 0;
    return new_contents;
}

int bsg_expect_file_contents(const char *path, const char *expected_contents, int expected_length) {
    FILE *fp = fopen(path, "rb");
    if (fp == NULL) {
        BUGSNAG_LOG("TEST: Error opening file %s: %s\n", path, strerror(errno));
        FAILm("bsg_expect_file_contents: Could not open file");
    }
    fseek(fp, 0L, SEEK_END);
    int observed_length = ftell(fp);
    fseek(fp, 0L, SEEK_SET);
    char* observed_contents = calloc(1, observed_length);
    if(observed_length > 0 && fread(observed_contents, observed_length, 1, fp) != 1) {
        FAILm("bsg_expect_file_contents: Error reading file");
    }

    if (!bsg_do_mem_contents_match(expected_contents, expected_length, observed_contents, observed_length)) {
        const char *printable_expected = bsg_as_printable(expected_contents, expected_length);
        const char *printable_observed = bsg_as_printable(observed_contents, observed_length);
        BUGSNAG_LOG("Expected contents of file %s to be [%s] but observed [%s]", path, printable_expected, printable_observed);
        free((void*)printable_expected);
        free((void*)printable_observed);
        free(observed_contents);
        FAILm("bsg_expect_file_contents: File contents did not match");
    }
    free(observed_contents);
    PASS();
}
