#include "event_reader.h"

#include "../../event.h"
#include "../string.h"
#include "utils/logger.h"

#include <fcntl.h>
#include <malloc.h>
#include <string.h>
#include <sys/stat.h>
#include <unistd.h>

const int BSG_MIGRATOR_CURRENT_VERSION = 14;

void bsg_read_feature_flags(int fd, bool expect_verification,
                            bsg_feature_flag **out_feature_flags,
                            size_t *out_feature_flag_count);

void bsg_read_opaque_metadata(int fd, bugsnag_metadata *metadata);

void bsg_read_opaque_breadcrumb_metadata(int fd,
                                         bugsnag_breadcrumb *breadcrumbs,
                                         int crumb_count);

bool bsg_read_breadcrumbs(int fd, bugsnag_event *event);

bsg_report_header *bsg_report_header_read(int fd) {
  bsg_report_header *header = calloc(1, sizeof(bsg_report_header));
  ssize_t len = read(fd, header, sizeof(bsg_report_header));
  if (len != sizeof(bsg_report_header)) {
    free(header);
    return NULL;
  }

  return header;
}

bugsnag_event *bsg_read_event(char *filepath) {
  int fd = open(filepath, O_RDONLY);
  if (fd == -1) {
    return NULL;
  }

  bsg_report_header *header = bsg_report_header_read(fd);
  if (header == NULL) {
    return NULL;
  }

  int version = header->version;
  free(header);

  if (version != BSG_MIGRATOR_CURRENT_VERSION) {
    return NULL;
  }

  size_t event_size = sizeof(bugsnag_event);
  bugsnag_event *event = calloc(1, event_size);

  ssize_t len = read(fd, event, event_size);
  if (len != event_size) {
    goto error;
  }

  // read the breadcrumbs
  if (!bsg_read_breadcrumbs(fd, event)) {
    goto error;
  }
  // read the feature flags, if possible
  bsg_read_feature_flags(fd, true, &event->feature_flags,
                         &event->feature_flag_count);
  bsg_read_opaque_metadata(fd, &event->metadata);
  bsg_read_opaque_breadcrumb_metadata(fd, event->breadcrumbs,
                                      event->crumb_count);

  return event;
error:
  free(event);
  return NULL;
}

bool bsg_read_breadcrumbs(int fd, bugsnag_event *event) {
  bugsnag_breadcrumb *breadcrumbs =
      calloc(event->max_crumb_count, sizeof(bugsnag_breadcrumb));
  if (breadcrumbs == NULL) {
    goto error;
  }
  const size_t bytes_to_read =
      event->max_crumb_count * sizeof(bugsnag_breadcrumb);
  const ssize_t read_count = read(fd, breadcrumbs, bytes_to_read);
  if (read_count != bytes_to_read) {
    goto error;
  }

  event->breadcrumbs = breadcrumbs;
  return true;
error:
  event->breadcrumbs = NULL;
  event->crumb_count = 0;
  event->crumb_first_index = 0;
  return false;
}

static char *read_string(int fd) {
  ssize_t len;
  uint32_t string_length;
  len = read(fd, &string_length, sizeof(string_length));

  if (len != sizeof(string_length)) {
    return NULL;
  }

  // allocate enough space, zero filler, and with a trailing '\0' terminator
  char *string_buffer = calloc(1, (string_length + 1));
  if (!string_buffer) {
    return NULL;
  }

  len = read(fd, string_buffer, string_length);
  if (len != string_length) {
    free(string_buffer);
    return NULL;
  }

  return string_buffer;
}

int read_byte(int fd) {
  char value;
  if (read(fd, &value, 1) != 1) {
    return -1;
  }

  return value;
}

void bsg_read_feature_flags(int fd, bool expect_verification,
                            bsg_feature_flag **out_feature_flags,
                            size_t *out_feature_flag_count) {

  ssize_t len;
  uint32_t feature_flag_count = 0;
  len = read(fd, &feature_flag_count, sizeof(feature_flag_count));
  if (len != sizeof(feature_flag_count)) {
    goto feature_flags_error;
  }

  bsg_feature_flag *flags =
      calloc(feature_flag_count, sizeof(bsg_feature_flag));
  for (uint32_t index = 0; index < feature_flag_count; index++) {
    char *name = read_string(fd);
    if (!name) {
      goto feature_flags_error;
    }

    int variant_exists = read_byte(fd);
    if (variant_exists < 0) {
      goto feature_flags_error;
    }

    char *variant = NULL;
    if (variant_exists) {
      variant = read_string(fd);
      if (!variant) {
        goto feature_flags_error;
      }
    }

    flags[index].name = name;
    flags[index].variant = variant;
  }

  if (expect_verification) {
    const uint8_t feature_flags_valid = read_byte(fd);
    if (feature_flags_valid != 0) {
      goto feature_flags_error;
    }
  }

  *out_feature_flag_count = feature_flag_count;
  *out_feature_flags = flags;

  return;

feature_flags_error:
  // something wrong - we release all allocated memory
  for (uint32_t index = 0; index < feature_flag_count; index++) {
    if (flags[index].name) {
      free(flags[index].name);
    }

    if (flags[index].variant) {
      free(flags[index].variant);
    }
  }

  free(flags);

  // clear the out fields to indicate no feature-flags are available
  *out_feature_flag_count = 0;
  *out_feature_flags = NULL;
}

void bsg_read_opaque_metadata(int fd, bugsnag_metadata *metadata) {
  size_t read_index = 0;
  for (; read_index < metadata->value_count; read_index++) {
    if (metadata->values[read_index].type == BSG_METADATA_OPAQUE_VALUE &&
        metadata->values[read_index].opaque_value_size > 0) {

      size_t opaque_value_size = metadata->values[read_index].opaque_value_size;

      void *opaque_value = calloc(1, opaque_value_size);
      if (opaque_value == NULL) {
        goto opaque_metadata_fail;
      }

      if (read(fd, opaque_value, opaque_value_size) != opaque_value_size) {
        free(opaque_value);
        goto opaque_metadata_fail;
      }

      metadata->values[read_index].opaque_value_size = opaque_value_size;
      metadata->values[read_index].opaque_value = opaque_value;
    }
  }

  return;

opaque_metadata_fail:
  // ensure that only the OPAQUE values we read successfully are considered
  // "valid" this allows for a partial recovery of the OPAQUE data
  for (; read_index < metadata->value_count; read_index++) {
    if (metadata->values[read_index].type == BSG_METADATA_OPAQUE_VALUE) {
      // set all unread OPAQUE values to NONE as their opaque_values are invalid
      metadata->values[read_index].type = BSG_METADATA_NONE_VALUE;
      metadata->values[read_index].opaque_value_size = 0;
      metadata->values[read_index].opaque_value = NULL;
    }
  }
}

void bsg_read_opaque_breadcrumb_metadata(int fd,
                                         bugsnag_breadcrumb *breadcrumbs,
                                         int crumb_count) {

  for (int breadcrumb_index = 0; breadcrumb_index < crumb_count;
       breadcrumb_index++) {

    bsg_read_opaque_metadata(fd, &(breadcrumbs[breadcrumb_index].metadata));
  }
}

static bool read_from_file(int fd, ssize_t length, char *buffer) {
  ssize_t bytes_read = 0;
  ssize_t total_bytes_read = 0;
  while (total_bytes_read < length) {
    ssize_t bytes_to_read = length - total_bytes_read;
    if ((bytes_read = read(fd, buffer + total_bytes_read, bytes_to_read)) < 0) {
      return false;
    }
    total_bytes_read += bytes_read;
  }
  return true;
}

ssize_t bsg_read_text_file(const char *filename, char **buffer_pointer) {
  char *data = NULL;
  ssize_t length = 0;
  struct stat stats;
  int fd = open(filename, O_RDONLY);
  if (fd < 0) {
    goto fail;
  }
  if (fstat(fd, &stats) < 0) {
    goto fail;
  }
  length = (ssize_t)stats.st_size;
  data = malloc(length + 1);
  if (data == NULL) {
    goto fail;
  }
  if (!read_from_file(fd, length, data)) {
    goto fail;
  }
  data[length] = 0;
  *buffer_pointer = data;
  goto success;

fail:
  length = -1;
success:
  if (fd > 0) {
    close(fd);
  }
  if (length < 0) {
    free(data);
  }
  return length;
}
