//
// Created by Karl Stenerud on 06.10.21.
//

#include "bugsnag_crashtime_journal.h"
#include "ctjournal.h"
#include "utils/number_to_string.h"
#include "utils/path_builder.h"

#define KEY_ERROR_CLASS "errorClass"
#define KEY_EXCEPTIONS "exceptions"
#define KEY_FILE "file"
#define KEY_FRAME_ADDRESS "frameAddress"
#define KEY_ID "id"
#define KEY_IS_PC "isPC"
#define KEY_LINE_NUMBER "lineNumber"
#define KEY_LOAD_ADDRESS "loadAddress"
#define KEY_MESSAGE "message"
#define KEY_METHOD "method"
#define KEY_NAME "name"
#define KEY_STACKTRACE "stacktrace"
#define KEY_SYMBOL_ADDRESS "symbolAddress"
#define KEY_TYPE "type"

static void write_string(const char *name, const char *value) {
  bsg_pb_stack_string(name);
  bsg_ctjournal_set_string(bsg_pb_path(), value);
  bsg_pb_unstack();
}

static void write_double(const char *name, double value) {
  bsg_pb_stack_string(name);
  bsg_ctjournal_set_double(bsg_pb_path(), value);
  bsg_pb_unstack();
}

static void write_boolean(const char *name, bool value) {
  bsg_pb_stack_string(name);
  bsg_ctjournal_set_boolean(bsg_pb_path(), value);
  bsg_pb_unstack();
}

static void write_stack_frame(const bugsnag_stackframe *frame, bool is_pc) {
  write_double(KEY_FRAME_ADDRESS, frame->frame_address);
  write_double(KEY_SYMBOL_ADDRESS, frame->symbol_address);
  write_double(KEY_LOAD_ADDRESS, frame->load_address);
  write_double(KEY_LINE_NUMBER, frame->line_number);
  if (is_pc) {
    write_boolean(KEY_IS_PC, true);
  }
  if (frame->filename[0] != 0) {
    write_string(KEY_FILE, frame->filename);
  }
  if (frame->method[0] != 0) {
    write_string(KEY_METHOD, frame->method);
  } else {
    char buff[20] = "0x";
    bsg_hex64_to_string(frame->frame_address, buff + 2);
    write_string(KEY_METHOD, buff);
  }
}

static void write_exception(const bugsnag_event *event) {
  const bsg_error *exc = &event->error;
  bsg_pb_stack_string(KEY_EXCEPTIONS);
  bsg_pb_stack_int(0);

  write_string(KEY_ERROR_CLASS, exc->errorClass);
  write_string(KEY_MESSAGE, exc->errorMessage);
  write_string(KEY_TYPE, "c");

  bsg_pb_stack_string(KEY_STACKTRACE);
  // assuming that the initial frame is the program counter. This logic will
  // need to be revisited if (for example) we add more intelligent processing
  // for stack overflow-type errors, like discarding the top frames, which
  // would mean no stored frame is the program counter.
  if (exc->frame_count > 0) {
    write_stack_frame(&exc->stacktrace[0], true);
  }
  for (int iFrame = 1; iFrame < exc->frame_count; iFrame++) {
    bsg_pb_stack_int(iFrame);
    write_stack_frame(&exc->stacktrace[iFrame], false);
    bsg_pb_unstack();
  }

  bsg_pb_unstack();
  bsg_pb_unstack();
  bsg_pb_unstack();
}

bool bsg_crashtime_journal_init(const char *journal_path) {
  return bsg_ctjournal_init(journal_path);
}

bool bsg_crashtime_journal_store_event(const bugsnag_event *event) {
  bsg_pb_reset();
  write_exception(event);
  return true;
}
