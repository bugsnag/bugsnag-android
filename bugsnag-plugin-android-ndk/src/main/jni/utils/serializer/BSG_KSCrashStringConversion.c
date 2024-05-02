//
//  BSG_KSCrashStringConversion.c
//  Bugsnag
//
//  Created by Karl Stenerud on 31.05.22.
//  Copyright © 2022 Bugsnag Inc. All rights reserved.
//

#include "BSG_KSCrashStringConversion.h"
#include <math.h>
#include <memory.h>
#include <time.h>

// Max uint64 is 18446744073709551615
#define MAX_UINT64_DIGITS 20

size_t bsg_uint64_to_string(uint64_t value, char *dst) {
  if (value == 0) {
    dst[0] = '0';
    dst[1] = 0;
    return 1;
  }

  char buff[MAX_UINT64_DIGITS + 1];
  buff[sizeof(buff) - 1] = 0;
  size_t index = sizeof(buff) - 2;
  for (;;) {
    buff[index] = (value % 10) + '0';
    value /= 10;
    if (value == 0) {
      break;
    }
    index--;
  }

  size_t length = sizeof(buff) - index;
  memcpy(dst, buff + index, length);
  return length - 1;
}

size_t bsg_int64_to_string(int64_t value, char *dst) {
  if (value < 0) {
    dst[0] = '-';
    return bsg_uint64_to_string((uint64_t)-value, dst + 1) + 1;
  }
  return bsg_uint64_to_string((uint64_t)value, dst);
}

static char g_hexNybbles[] = {'0', '1', '2', '3', '4', '5', '6', '7',
                              '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};

size_t bsg_uint64_to_hex(uint64_t value, char *dst, int min_digits) {
  if (min_digits < 1) {
    min_digits = 1;
  } else if (min_digits > 16) {
    min_digits = 16;
  }

  char buff[MAX_UINT64_DIGITS + 1];
  buff[sizeof(buff) - 1] = 0;
  size_t index = sizeof(buff) - 2;
  for (int digitCount = 1;; digitCount++) {
    buff[index] = g_hexNybbles[(value & 15)];
    value >>= 4;
    if (value == 0 && digitCount >= min_digits) {
      break;
    }
    index--;
  }

  size_t length = sizeof(buff) - index;
  memcpy(dst, buff + index, length);
  return length - 1;
}

/**
 * Convert a positive double to a string, allowing up to max_sig_digits.
 * To reduce the complexity of this algorithm, values with an exponent
 * other than 0 are always printed in exponential form.
 *
 * Values are rounded half-up.
 *
 * This function makes use of compiler intrinsic functions which, though not
 * officially async-safe, are actually async-safe (no allocations, locks, etc).
 *
 * This function will write a maximum of 21 characters (including the NUL) to
 * dst.
 *
 * Returns the length of the string written to dst (not including the NUL).
 */
static size_t positive_double_to_string(const double value, char *dst,
                                        int max_sig_digits) {
  const char *const orig_dst = dst;
  if (max_sig_digits > 16) {
    max_sig_digits = 16;
  }

  if (value == 0) {
    dst[0] = '0';
    dst[1] = 0;
    return 1;
  }

  // isnan() is basically ((x) != (x))
  if (isnan(value)) {
    strlcpy(dst, "nan", 4);
    return 3;
  }

  // isinf() is a compiler intrinsic.
  if (isinf(value)) {
    strlcpy(dst, "inf", 4);
    return 3;
  }

  // log10() is a compiler intrinsic.
  int exponent = (int)log10(value);
  // Values < 1.0 must subtract 1 from exponent to handle zero wraparound.
  if (value < 1.0) {
    exponent--;
  }

  // pow() is a compiler intrinsic.
  double normalized = value / pow(10, exponent);
  // Special case for 0.1, 0.01, 0.001, etc giving a normalized value of 10.xyz.
  // We use 9.999... because 10.0 converts to a value > 10 in ieee754 binary
  // floats.
  if (normalized > 9.99999999999999822364316059975) {
    exponent++;
    normalized = value / pow(10, exponent);
  }

  // Put all of the digits we'll use into an integer.
  double digits_and_remainder = normalized * pow(10, max_sig_digits - 1);
  uint64_t digits = (uint64_t)digits_and_remainder;
  // Also round up if necessary (note: 0.5 is exact in both binary and decimal).
  if (digits_and_remainder - (double)digits >= 0.5) {
    digits++;
    // Special case: Adding one bumps us to next magnitude.
    if (digits >= (uint64_t)pow(10, max_sig_digits)) {
      exponent++;
      digits /= 10;
    }
  }

  // Extract the fractional digits.
  for (int i = max_sig_digits; i > 1; i--) {
    dst[i] = digits % 10 + '0';
    digits /= 10;
  }
  // Extract the single-digit whole part.
  dst[0] = (char)digits + '0';
  dst[1] = '.';

  // Strip off trailing zeroes, and also the '.' if there is no fractional part.
  int e_offset = max_sig_digits;
  for (int i = max_sig_digits; i > 0; i--) {
    if (dst[i] != '0') {
      if (dst[i] == '.') {
        e_offset = i;
      } else {
        e_offset = i + 1;
      }
      break;
    }
  }
  dst += e_offset;

  // Add the exponent if it's not 0.
  if (exponent != 0) {
    *dst++ = 'e';
    if (exponent >= 0) {
      *dst++ = '+';
    }
    dst += bsg_int64_to_string(exponent, dst);
  } else {
    *dst = 0;
  }

  return (size_t)(dst - orig_dst);
}

size_t bsg_double_to_string(double value, char *dst, int max_sig_digits) {
  if (max_sig_digits < 1) {
    max_sig_digits = 1;
  }
  if (value < 0) {
    dst[0] = '-';
    return positive_double_to_string(-value, dst + 1, max_sig_digits) + 1;
  }
  return positive_double_to_string(value, dst, max_sig_digits);
}

// async-safe gmtime_r implementation
static void safe_gmtime_r(time_t time, struct tm *out) {
  const int seconds_per_day = 86400;
  const int days_per_400years = 365 * 400 + 97;
  const int days_per_100years = 365 * 100 + 24;
  const int days_in_month_leap_year[] = {31, 29, 31, 30, 31, 30,
                                         31, 31, 30, 31, 30, 31};
  const int days_in_month_non_leap_year[] = {31, 28, 31, 30, 31, 30,
                                             31, 31, 30, 31, 30, 31};
  const int days_per_4years = 365 * 4 + 1;

  int days = time / seconds_per_day;
  int secs = time % seconds_per_day;
  int years = 1970;

  int quotient = days / days_per_400years;
  days -= quotient * days_per_400years;
  years += quotient * 400;

  quotient = days / days_per_100years;
  days -= quotient * days_per_100years;
  years += quotient * 100;

  quotient = days / days_per_4years;
  days -= quotient * days_per_4years;
  years += quotient * 4;

  quotient = days / 365;
  days -= quotient * 365;
  years += quotient;

  out->tm_year = years - 1900;
  out->tm_yday = days;

  const int *days_per_month =
      out->tm_year % 4 == 0
          ? (out->tm_year % 100 == 0
                 ? (out->tm_year % 400 == 0 ? days_in_month_leap_year
                                            : days_in_month_non_leap_year)
                 : days_in_month_leap_year)
          : days_in_month_non_leap_year;

  int month = 0;
  while (days >= days_per_month[month]) {
    days -= days_per_month[month];
    month++;
  }

  out->tm_mon = month;
  out->tm_mday = days + 1;

  out->tm_hour = secs / 3600;
  secs %= 3600;
  out->tm_min = secs / 60;
  out->tm_sec = secs % 60;
}

size_t bsg_time_to_simplified_iso8601_string(time_t time, char *dst) {
  struct tm tm;
  safe_gmtime_r(time, &tm);
  size_t length = 0;
  length += bsg_uint64_to_string(tm.tm_year + 1900, dst + length);
  dst[length++] = '-';
  if (tm.tm_mon + 1 < 10)
    dst[length++] = '0';
  length += bsg_uint64_to_string(tm.tm_mon + 1, dst + length);
  dst[length++] = '-';
  if (tm.tm_mday < 10)
    dst[length++] = '0';
  length += bsg_uint64_to_string(tm.tm_mday, dst + length);
  dst[length++] = 'T';
  if (tm.tm_hour < 10)
    dst[length++] = '0';
  length += bsg_uint64_to_string(tm.tm_hour, dst + length);
  dst[length++] = ':';
  if (tm.tm_min < 10)
    dst[length++] = '0';
  length += bsg_uint64_to_string(tm.tm_min, dst + length);
  dst[length++] = ':';
  if (tm.tm_sec < 10)
    dst[length++] = '0';
  length += bsg_uint64_to_string(tm.tm_sec, dst + length);
  dst[length++] = 'Z';
  dst[length] = '\0';
  return length;
}
