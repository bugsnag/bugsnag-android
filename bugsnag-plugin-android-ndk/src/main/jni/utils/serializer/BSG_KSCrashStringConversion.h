//
//  BSG_KSCrashStringConversion.h
//  Bugsnag
//
//  Created by Karl Stenerud on 31.05.22.
//  Copyright © 2022 Bugsnag Inc. All rights reserved.
//

#ifndef BSG_KSCrashStringConversion_h
#define BSG_KSCrashStringConversion_h

#include <stddef.h>
#include <stdint.h>
#include <sys/types.h>

#ifdef __cplusplus
extern "C" {
#endif

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
 * Convert an unsigned integer to a hex string.
 * This will write a maximum of 17 characters (including the NUL) to dst.
 *
 * If min_digits is greater than 1, it will prepad with zeroes to reach this
 * number of digits (up to a maximum of 16 digits).
 *
 * Returns the length of the string written to dst (not including the NUL).
 */
size_t bsg_uint64_to_hex(uint64_t value, char *dst, int min_digits);

/**
 * Convert a positive double to a string, allowing up to max_sig_digits.
 * To reduce the complexity of this algorithm, values with an exponent
 * other than 0 are always printed in exponential form.
 *
 * Values are rounded half-up, and thus will differ slightly from printf.
 *
 * This function makes use of compiler intrinsic functions which, though not
 * officially async-safe, are actually async-safe (no allocations, locks, etc).
 *
 * Note: Double conversion is not intended to be round-trippable.
 *      It is 99.99% correct but has subtle differences from printf.
 *
 * This function will write a maximum of 22 characters (including the NUL) to
 * dst.
 *
 * max_sig_digits is capped between 1 and 16 (inclusive) because that's the
 * range of significant digits an ieee754 binary float64 can represent.
 *
 * Returns the length of the string written to dst (not including the NUL).
 */
size_t bsg_double_to_string(double value, char *dst, int max_sig_digits);

/**
 * Convert a time to a simplified ISO8601 date string. No timezone information
 * is added, and the output date is always in UTC.
 *
 * Returns the length of the string written to dst (not including the null
 * termination byte).
 */
size_t bsg_time_to_simplified_iso8601_string(time_t time, char *dst);

#ifdef __cplusplus
}
#endif

#endif /* BSG_KSCrashStringConversion_h */