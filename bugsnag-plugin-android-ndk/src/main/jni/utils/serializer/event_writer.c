#include "event_writer.h"

#include <fcntl.h>
#include <unistd.h>

#include "../string.h"
#include "buffered_writer.h"

bool bsg_write_feature_flags(bugsnag_event *event, bsg_buffered_writer *writer);

bool bsg_write_opaque_metadata(bugsnag_event *event,
                               bsg_buffered_writer *writer);

bool bsg_report_header_write(bsg_report_header *header, int fd) {
  ssize_t len = write(fd, header, sizeof(bsg_report_header));

  return len == sizeof(bsg_report_header);
}

bool bsg_event_write(bsg_environment *env) {
  bsg_buffered_writer writer;
  if (!bsg_buffered_writer_open(&writer, env->next_event_path)) {
    return false;
  }

  bool result =
      // write header - determines format version, etc
      bsg_report_header_write(&env->report_header, writer.fd) &&
      // add cached event info
      writer.write(&writer, &env->next_event, sizeof(bugsnag_event)) &&
      // append feature flags after event structure
      bsg_write_feature_flags(&env->next_event, &writer) &&
      // append opaque metadata after the feature flags
      bsg_write_opaque_metadata(&env->next_event, &writer);

  writer.dispose(&writer);
  return result;
}

bool bsg_lastrun_write(bsg_environment *env) {
  char *path = env->last_run_info_path;
  int fd = open(path, O_WRONLY | O_CREAT | O_TRUNC, 0644);
  if (fd == -1) {
    return false;
  }

  int size = bsg_strlen(env->next_last_run_info);
  ssize_t len = write(fd, env->next_last_run_info, size);
  return len == size;
}

static bool write_feature_flag(bsg_buffered_writer *writer,
                               bsg_feature_flag *flag) {
  if (!writer->write_string(writer, flag->name)) {
    return false;
  }

  if (flag->variant) {
    if (!writer->write_byte(writer, 1)) {
      return false;
    }

    if (!writer->write_string(writer, flag->variant)) {
      return false;
    }
  } else {
    if (!writer->write_byte(writer, 0)) {
      return false;
    }
  }

  return true;
}

bool bsg_write_feature_flags(bugsnag_event *event,
                             bsg_buffered_writer *writer) {
  const uint32_t feature_flag_count = event->feature_flag_count;
  if (!writer->write(writer, &feature_flag_count, sizeof(feature_flag_count))) {
    return false;
  }

  for (uint32_t index = 0; index < feature_flag_count; index++) {
    if (!write_feature_flag(writer, &event->feature_flags[index])) {
      return false;
    }
  }

  return true;
}

static bool bsg_write_opaque_metadata_unit(bugsnag_metadata *metadata,
                                           bsg_buffered_writer *writer) {

  for (size_t index = 0; index < metadata->value_count; index++) {
    uint32_t value_size = metadata->values[index].opaque_value_size;
    if (metadata->values[index].type == BSG_METADATA_OPAQUE_VALUE &&
        value_size > 0) {
      if (!writer->write(writer, metadata->values[index].opaque_value,
                         value_size)) {
        return false;
      }
    }
  }

  return true;
}

bool bsg_write_opaque_metadata(bugsnag_event *event,
                               bsg_buffered_writer *writer) {

  if (!bsg_write_opaque_metadata_unit(&event->metadata, writer)) {
    return false;
  }

  for (int breadcrumb_index = 0; breadcrumb_index < event->crumb_count;
       breadcrumb_index++) {
    if (!bsg_write_opaque_metadata_unit(
            &event->breadcrumbs[breadcrumb_index].metadata, writer)) {
      return false;
    }
  }

  return true;
}
