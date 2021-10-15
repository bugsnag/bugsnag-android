//
// Created by Karl Stenerud on 06.10.21.
//

#include "bugsnag_crashtime_journal.h"
#include "ctjournal.h"
#include "utils/number_to_string.h"
#include "utils/path_builder.h"
#include <time.h>

#define KEY_ATTRIBUTES "attributes"
#define KEY_DEVICE "device"
#define KEY_ERROR_CLASS "errorClass"
#define KEY_EXCEPTIONS "exceptions"
#define KEY_EVENTS "events"
#define KEY_FILE "file"
#define KEY_FRAME_ADDRESS "frameAddress"
#define KEY_ID "id"
#define KEY_IS_PC "isPC"
#define KEY_LINE_NUMBER "lineNumber"
#define KEY_LOAD_ADDRESS "loadAddress"
#define KEY_MESSAGE "message"
#define KEY_METHOD "method"
#define KEY_NAME "name"
#define KEY_SESSION "session"
#define KEY_SEVERITY "severity"
#define KEY_SEVERITY_REASON "severityReason"
#define KEY_SIGNAL_TYPE "signalType"
#define KEY_STACKTRACE "stacktrace"
#define KEY_SYMBOL_ADDRESS "symbolAddress"
#define KEY_TIME "time"
#define KEY_TYPE "type"
#define KEY_UNHANDLED "unhandled"
#define KEY_UNHANDLED_OVERRIDDEN "unhandledOverridden"

static void add_string(const char *name, const char *value) {
  bsg_pb_stack_map_key(name);
  bsg_ctjournal_set_string(bsg_pb_path(), value);
  bsg_pb_unstack();
}

static void add_double(const char *name, double value) {
  bsg_pb_stack_map_key(name);
  bsg_ctjournal_set_double(bsg_pb_path(), value);
  bsg_pb_unstack();
}

static void add_int(const char *name, int64_t value) {
  add_double(name, value);
}

static void add_hex(const char *name, uint64_t value) {
  char buff[20] = "0x";
  bsg_hex64_to_string(value, buff + 2);
  add_string(name, buff);
}

static void add_boolean(const char *name, bool value) {
  bsg_pb_stack_map_key(name);
  bsg_ctjournal_set_boolean(bsg_pb_path(), value);
  bsg_pb_unstack();
}

static void stack_next_map_entry() {
  bsg_pb_stack_new_list_entry();
  bsg_ctjournal_set_map(bsg_pb_path());
  bsg_pb_unstack();
  bsg_pb_stack_list_index(-1);
}

static void add_stack_frame(const bugsnag_stackframe *frame, bool is_pc) {
  stack_next_map_entry();
  add_hex(KEY_FRAME_ADDRESS, frame->frame_address);
  add_hex(KEY_SYMBOL_ADDRESS, frame->symbol_address);
  add_hex(KEY_LOAD_ADDRESS, frame->load_address);
  add_double(KEY_LINE_NUMBER, frame->line_number);
  if (is_pc) {
    add_boolean(KEY_IS_PC, true);
  }
  if (frame->filename[0] != 0) {
    add_string(KEY_FILE, frame->filename);
  }
  if (frame->method[0] != 0) {
    add_string(KEY_METHOD, frame->method);
  } else {
    add_hex(KEY_METHOD, frame->frame_address);
  }
  bsg_pb_unstack();
}

static void add_exception(const bugsnag_event *event) {
  const bsg_error *exc = &event->error;
  bsg_pb_stack_map_key(KEY_EXCEPTIONS);
  stack_next_map_entry();

  add_string(KEY_ERROR_CLASS, exc->errorClass);
  add_string(KEY_MESSAGE, exc->errorMessage);
  add_string(KEY_TYPE, "c");

  bsg_pb_stack_map_key(KEY_STACKTRACE);
  // assuming that the initial frame is the program counter. This logic will
  // need to be revisited if (for example) we add more intelligent processing
  // for stack overflow-type errors, like discarding the top frames, which
  // would mean no stored frame is the program counter.
  if (exc->frame_count > 0) {
    add_stack_frame(&exc->stacktrace[0], true);
  }
  for (int iFrame = 1; iFrame < exc->frame_count; iFrame++) {
    add_stack_frame(&exc->stacktrace[iFrame], false);
  }

  bsg_pb_unstack();
  bsg_pb_unstack();
  bsg_pb_unstack();
}

const char *bsg_severity_string(bugsnag_severity type);

static void add_severity_reason_attrs(const bugsnag_event *event) {
  bsg_pb_stack_map_key(KEY_ATTRIBUTES);
  add_string(KEY_SIGNAL_TYPE, event->error.errorClass);
  bsg_pb_unstack();
}

static void add_severity_reason_obj(const bugsnag_event *event) {
  bsg_pb_stack_map_key(KEY_SEVERITY_REASON);

  // unhandled == false always means that the state has been overridden by the
  // user, as this codepath is only executed for unhandled native errors
  add_boolean(KEY_UNHANDLED_OVERRIDDEN, !event->unhandled);

  // FUTURE(dm): severityReason/unhandled attributes are currently
  // over-optimized for signal handling. in the future we may want to handle
  // C++ exceptions, etc as well.
  add_string(KEY_TYPE, "signal");
  add_severity_reason_attrs(event);
  bsg_pb_unstack();
}

static void add_severity_reason(const bugsnag_event *event) {
  const char *value = bsg_severity_string(event->severity);
  add_string(KEY_SEVERITY, value);
  add_boolean(KEY_UNHANDLED, event->unhandled);
  add_severity_reason_obj(event);
}

static void add_device_time(const bugsnag_event *event) {
  bsg_pb_stack_map_key(KEY_DEVICE);

  char event_time[sizeof("2018-10-08T12:07:09Z")];
  time_t now = event->device.time;
  if (now > 0) {
    strftime(event_time, sizeof(event_time), "%FT%TZ", gmtime(&now));
  }

  add_string(KEY_TIME, event_time);
  bsg_pb_unstack();
}

static void add_session(const bugsnag_event *event) {
  if (bugsnag_event_has_session(event)) {
    bsg_pb_stack_map_key(KEY_SESSION);
    bsg_pb_stack_map_key(KEY_EVENTS);
    add_int(KEY_UNHANDLED, event->unhandled_events);
    bsg_pb_unstack();
    bsg_pb_unstack();
  }
}

bool bsg_crashtime_journal_init(const char *journal_path) {
  return bsg_ctjournal_init(journal_path);
}

bool bsg_crashtime_journal_store_event(const bugsnag_event *event) {
  bsg_pb_reset();
  add_exception(event);
  add_severity_reason(event);
  add_device_time(event);
  add_session(event);
  return true;
}
