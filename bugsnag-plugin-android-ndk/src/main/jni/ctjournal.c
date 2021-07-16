#include "bugsnag_ndk.h"
#include "utils/buffered_writer.h"
#include "utils/number_to_string.h"
#include "utils/string.h"
#include <errno.h>
#include <fcntl.h>
#include <math.h>
#include <string.h>
#include <sys/stat.h>
#include <unistd.h>

/* Crash-time journal writing functions. These must be async-safe and fast,
 * which is why they use low-level open() and write() for everything, and make
 * no calls to formatted output functions like sprintf.
 *
 * Journal writing functions end writing just after the opening quote whenever
 * strings are involved. So for example, a function that leads up to writing a
 * map key will end just after the '"', not before. The same goes for higher
 * level string writing functions (not including write_string, write_raw_string,
 * write_raw_bytes, and write_raw_literal).
 *
 * There are two versions of some functions to handle the requirement to close a
 * quoted string, or to set up for a map.
 */

/* IMPORTANT: Keep this consistent with:
 * - bugsnag_breadcrumb_type in
 *       bugsnag-plugin-android-ndk/src/main/assets/include/event.h
 * - BreadcrumbType in
 *       bugsnag-android-core/src/main/java/com/bugsnag/android/BreadcrumbType.kt
 */
static const char *g_breadcrumb_types[] = {
    "manual",     // BSG_CRUMB_MANUAL
    "error",      // BSG_CRUMB_ERROR
    "log",        // BSG_CRUMB_LOG
    "navigation", // BSG_CRUMB_NAVIGATION
    "process",    // BSG_CRUMB_PROCESS
    "request",    // BSG_CRUMB_REQUEST
    "state",      // BSG_CRUMB_STATE
    "user",       // BSG_CRUMB_USER
};
static const int g_breadcrumb_types_count =
    sizeof(g_breadcrumb_types) / sizeof(*g_breadcrumb_types);

// The buffer size for the buffered writer
#define BUFFER_SIZE (1024 * 128)

// Implemented as defines to take advantage of compile-time size calculation
// See write_raw_literal()
#define PATH_METADATA_PREFIX "events.-1.metaData."
#define PATH_NEXT_BREADCRUMB "events.-1.breadcrumbs."
#define PATH_LAST_BREADCRUMB_METADATA_PREFIX                                   \
  "events.-1.breadcrumbs.-1.metaData."
#define PATH_USER "events.-1.user"
#define KEY_ID "id"
#define KEY_EMAIL "email"
#define KEY_NAME "name"
#define KEY_TIMESTAMP "timestamp"
#define KEY_TYPE "type"

/* The maximum number of significant digits when printing floats.
 * 7 (6 + 1 whole digit in exp form) is the default used by the sprintf code.
 */
#define MAX_SIGNIFICANT_DIGITS 7

// Single journal writer (there can only be one crash-time journal)
static bsg_buffered_writer *g_writer;

/**
 * Convenience macro to stop the current function and return false if a call
 * returns false.
 */
#define RETURN_ON_FALSE(...)                                                   \
  do {                                                                         \
    if (!(__VA_ARGS__)) {                                                      \
      return false;                                                            \
    }                                                                          \
  } while (0)

static inline bool write_raw_bytes(const char *bytes, size_t length) {
  if (!g_writer->write(g_writer, bytes, length)) {
    BUGSNAG_LOG("Error writing to journal at %s: %s", g_writer->path,
                strerror(errno));
    return false;
  }
  return true;
}

static inline bool flush_writer() {
  if (!g_writer->flush(g_writer)) {
    BUGSNAG_LOG("Error flushing to journal at %s: %s", g_writer->path,
                strerror(errno));
    return false;
  }
  return true;
}

static inline bool write_raw_string(const char *string) {
  return write_raw_bytes(string, strlen(string));
}

/**
 * This is implemented as a macro to take advantage of compile-time size
 * calculations when passing in string literals.
 */
#define write_raw_literal(LITERAL_STRING)                                      \
  write_raw_bytes((LITERAL_STRING), sizeof(LITERAL_STRING) - 1)

// Marks which characters require escaping.
static const bool escape_requiring_chars[0x100] = {
    ['"'] = true,
    ['\\'] = true,
};

static inline bool requires_escapes(const char *string) {
  while (*string) {
    if (escape_requiring_chars[*string]) {
      return true;
    }
    string++;
  }
  return false;
}

static inline bool write_escaped(const char *string) {
  // Use a small buffer size to avoid stack contention as this is the unlikely
  // path.
  char buff[50];
  // Use a high-water value that takes into account escape char stuffing
  const size_t buff_hiwater_marker = sizeof(buff) - 1;
  size_t i_buff = 0;

  while (*string) {
    if (escape_requiring_chars[*string]) {
      buff[i_buff++] = '\\';
    }
    buff[i_buff++] = *string;
    if (i_buff >= buff_hiwater_marker) {
      if (!write_raw_bytes(buff, i_buff)) {
        return false;
      }
      i_buff = 0;
    }
    string++;
  }

  if (i_buff > 0) {
    if (!write_raw_bytes(buff, i_buff)) {
      return false;
    }
  }

  return true;
}

static inline bool write_string(const char *string) {
  if (!requires_escapes(string)) {
    return write_raw_bytes(string, strlen(string));
  }
  return write_escaped(string);
}

static inline bool write_boolean(bool value) {
  if (value) {
    return write_raw_literal("true");
  } else {
    return write_raw_literal("false");
  }
}

static inline bool write_null() { return write_raw_literal("null"); }

// Max significant digits + special chars for sign, exponent, etc.
#define MAX_STRING_SIZE_FLOAT 22

static inline bool write_double(double value) {
  char buff[MAX_STRING_SIZE_FLOAT];
  bsg_double_to_string(value, buff, MAX_SIGNIFICANT_DIGITS);
  return write_raw_string(buff);
}

static inline bool write_journal_begin_common(const char *path,
                                              const char *separator) {
  RETURN_ON_FALSE(write_raw_literal("{\""));
  RETURN_ON_FALSE(write_string(path));
  return write_raw_string(separator);
}

static inline bool write_journal_begin_string(const char *path) {
  return write_journal_begin_common(path, "\":\"");
}

static inline bool write_journal_begin_nonstring(const char *path) {
  return write_journal_begin_common(path, "\":");
}

static inline bool write_journal_mapentry_string(const char *key,
                                                 const char *value,
                                                 bool hasMoreEntries) {
  RETURN_ON_FALSE(write_string(key));
  RETURN_ON_FALSE(write_raw_literal("\":\""));
  RETURN_ON_FALSE(write_string(value));
  if (hasMoreEntries) {
    RETURN_ON_FALSE(write_raw_literal("\",\""));
  } else {
    RETURN_ON_FALSE(write_raw_literal("\""));
  }
  return true;
}

static inline bool write_journal_begin_map(const char *path) {
  return write_journal_begin_common(path, "\":{\"");
}

static inline bool write_journal_end_string() {
  return write_raw_bytes("\"}\0", 3);
}

static inline bool write_journal_end_nonstring() {
  return write_raw_bytes("}\0", 2);
}

static inline bool write_journal_end_map() {
  return write_raw_bytes("}}\0", 3);
}

static inline bool write_metadata_begin_common(const char *section,
                                               const char *name,
                                               const char *separator) {
  RETURN_ON_FALSE(write_raw_literal("{\"" PATH_METADATA_PREFIX));
  RETURN_ON_FALSE(write_string(section));
  if (name != NULL) {
    RETURN_ON_FALSE(write_raw_literal("."));
    RETURN_ON_FALSE(write_string(name));
  }
  return write_raw_string(separator);
}

static inline bool write_metadata_begin_string(const char *section,
                                               const char *name) {
  return write_metadata_begin_common(section, name, "\":\"");
}

static inline bool write_metadata_begin_nonstring(const char *section,
                                                  const char *name) {
  return write_metadata_begin_common(section, name, "\":");
}

// API

bool bsg_ctjournal_clear_value(const char *path) {
  if (path == NULL) {
    return false;
  }

  RETURN_ON_FALSE(write_journal_begin_nonstring(path));
  RETURN_ON_FALSE(write_null());
  RETURN_ON_FALSE(write_journal_end_nonstring());

  return flush_writer();
}

bool bsg_ctjournal_set_double(const char *path, double value) {
  if (path == NULL) {
    return false;
  }

  RETURN_ON_FALSE(write_journal_begin_nonstring(path));
  RETURN_ON_FALSE(write_double(value));
  RETURN_ON_FALSE(write_journal_end_nonstring());

  return flush_writer();
}

bool bsg_ctjournal_set_boolean(const char *path, bool value) {
  if (path == NULL) {
    return false;
  }

  RETURN_ON_FALSE(write_journal_begin_nonstring(path));
  RETURN_ON_FALSE(write_boolean(value));
  RETURN_ON_FALSE(write_journal_end_nonstring());

  return flush_writer();
}

bool bsg_ctjournal_set_string(const char *path, const char *value) {
  if (path == NULL) {
    return false;
  }

  if (value == NULL) {
    return bsg_ctjournal_clear_value(path);
  }

  RETURN_ON_FALSE(write_journal_begin_string(path));
  RETURN_ON_FALSE(write_string(value));
  RETURN_ON_FALSE(write_journal_end_string());

  return flush_writer();
}

bool bsg_ctjournal_metadata_clear_section(const char *section) {
  if (section == NULL) {
    return false;
  }

  RETURN_ON_FALSE(write_metadata_begin_nonstring(section, NULL));
  RETURN_ON_FALSE(write_null());
  RETURN_ON_FALSE(write_journal_end_nonstring());

  return flush_writer();
}

bool bsg_ctjournal_metadata_clear_value(const char *section, const char *name) {
  if (section == NULL || name == NULL) {
    return false;
  }

  RETURN_ON_FALSE(write_metadata_begin_nonstring(section, name));
  RETURN_ON_FALSE(write_null());
  RETURN_ON_FALSE(write_journal_end_nonstring());

  return flush_writer();
}

bool bsg_ctjournal_metadata_set_boolean(const char *section, const char *name,
                                        bool value) {
  if (section == NULL || name == NULL) {
    return false;
  }

  RETURN_ON_FALSE(write_metadata_begin_nonstring(section, name));
  RETURN_ON_FALSE(write_boolean(value));
  RETURN_ON_FALSE(write_journal_end_nonstring());

  return flush_writer();
}

bool bsg_ctjournal_metadata_set_double(const char *section, const char *name,
                                       double value) {
  if (section == NULL || name == NULL) {
    return false;
  }

  RETURN_ON_FALSE(write_metadata_begin_nonstring(section, name));
  RETURN_ON_FALSE(write_double(value));
  RETURN_ON_FALSE(write_journal_end_nonstring());

  return flush_writer();
}

bool bsg_ctjournal_metadata_set_string(const char *section, const char *name,
                                       const char *value) {
  if (section == NULL || name == NULL) {
    return false;
  }

  RETURN_ON_FALSE(write_metadata_begin_string(section, name));
  RETURN_ON_FALSE(write_string(value));
  RETURN_ON_FALSE(write_journal_end_string());

  return flush_writer();
}

bool bsg_ctjournal_set_user(const char *id, const char *email,
                            const char *name) {
  RETURN_ON_FALSE(write_journal_begin_map(PATH_USER));
  RETURN_ON_FALSE(write_journal_mapentry_string(KEY_ID, id, true));
  RETURN_ON_FALSE(write_journal_mapentry_string(KEY_EMAIL, email, true));
  RETURN_ON_FALSE(write_journal_mapentry_string(KEY_NAME, name, false));
  RETURN_ON_FALSE(write_journal_end_map());

  return flush_writer();
}

bool bsg_ctjournal_add_breadcrumb(const bugsnag_breadcrumb *breadcrumb) {
  if (breadcrumb->type >= g_breadcrumb_types_count) {
    return false;
  }

  // We write this as a main journal entry + 1 entry per metadata value.

  RETURN_ON_FALSE(write_journal_begin_map(PATH_NEXT_BREADCRUMB));
  RETURN_ON_FALSE(
      write_journal_mapentry_string(KEY_NAME, breadcrumb->name, true));
  RETURN_ON_FALSE(write_journal_mapentry_string(KEY_TIMESTAMP,
                                                breadcrumb->timestamp, true));
  RETURN_ON_FALSE(write_journal_mapentry_string(
      KEY_TYPE, g_breadcrumb_types[breadcrumb->type], false));
  RETURN_ON_FALSE(write_journal_end_map());

  for (int i = 0; i < breadcrumb->metadata.value_count; i++) {
    const bsg_metadata_value *meta = &breadcrumb->metadata.values[i];
    RETURN_ON_FALSE(write_raw_literal("{\""));
    RETURN_ON_FALSE(write_raw_literal(PATH_LAST_BREADCRUMB_METADATA_PREFIX));
    RETURN_ON_FALSE(write_string(meta->section));
    RETURN_ON_FALSE(write_raw_literal("."));
    RETURN_ON_FALSE(write_string(meta->name));

    switch (meta->type) {
    case BSG_METADATA_NONE_VALUE:
      RETURN_ON_FALSE(write_raw_literal("\":"));
      RETURN_ON_FALSE(write_null());
      RETURN_ON_FALSE(write_journal_end_nonstring());
      break;
    case BSG_METADATA_BOOL_VALUE:
      RETURN_ON_FALSE(write_raw_literal("\":"));
      RETURN_ON_FALSE(write_boolean(meta->bool_value));
      RETURN_ON_FALSE(write_journal_end_nonstring());
      break;
    case BSG_METADATA_CHAR_VALUE:
      RETURN_ON_FALSE(write_raw_literal("\":\""));
      RETURN_ON_FALSE(write_string(meta->char_value));
      RETURN_ON_FALSE(write_journal_end_string());
      break;
    case BSG_METADATA_NUMBER_VALUE:
      RETURN_ON_FALSE(write_raw_literal("\":"));
      RETURN_ON_FALSE(write_double(meta->double_value));
      RETURN_ON_FALSE(write_journal_end_nonstring());
      break;
    }
  }

  return flush_writer();
}

bool bsg_ctjournal_init(const char *journal_path) {
  // We don't need strong concurrency protections here because we're only going
  // to call this once. If it did get called more than once AND raced, we'd have
  // an extra dangling fd and a small amount of leaked memory - no harm done.
  if (g_writer != NULL) {
    return true;
  }

  g_writer = bsg_buffered_writer_open(BUFFER_SIZE, journal_path);
  if (g_writer == NULL) {
    BUGSNAG_LOG("Could not open journal file %s: %s", journal_path,
                strerror(errno));
    return false;
  }

  // Initial journal info entry:
  RETURN_ON_FALSE(write_raw_literal(
      "{\"*\":{\"type\":\"Bugsnag Journal\",\"version\":1}}\0"));

  return flush_writer();
}

/**
 * For unit tests only: Reset the journal system.
 */
void bsg_ctjournal_test_reset() {
  if (g_writer == NULL) {
    return;
  }

  g_writer->dispose(g_writer);
  g_writer = NULL;
}
