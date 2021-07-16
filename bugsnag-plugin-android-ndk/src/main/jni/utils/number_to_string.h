/**
 * Async-safe number to string conversion functions.
 */

#ifndef BUGSNAG_NUMBER_TO_STRING_H
#define BUGSNAG_NUMBER_TO_STRING_H

#include <stdint.h>

/**
 * Convert an unsigned integer to a string.
 * This will write a maximum of 21 characters (including the NUL) to dst.
 *
 * Returns the length of the string written to dst (not including the NUL).
 */
size_t bsg_uint64_to_string(uint64_t value, char *dst);

/**
 * Convert an integer to a string.
 * This will write a maximum of 22 characters (including the null terminator) to
 * dst.
 *
 * Returns the length of the string written to dst (not including the null
 * termination byte).
 */
size_t bsg_int64_to_string(int64_t value, char *dst);

/**
 * Convert a double to a string, allowing up to max_sig_digits. See
 * positive_double_to_string() for important information about how this
 * algorithm differs from sprintf.
 *
 * This function will write a maximum of 22 characters (including the NUL) to
 * dst.
 *
 * Returns the length of the string written to dst (not including the NUL).
 */
size_t bsg_double_to_string(double value, char *dst, int max_sig_digits);

// The default number of significant digits when printing floats, according to
// sprintf().
#define BSG_DEFAULT_SIGNIFICANT_DIGITS 7

#endif
