#include "../utils/build.h"
#include "stack_unwinder_libunwind.h"
#include <malloc.h>
#include <report.h>
#include <unwind.h>

typedef struct {
  size_t frame_count;
  uintptr_t frame_addresses[BUGSNAG_FRAMES_MAX];
} bsg_libunwind_state;

bsg_libunwind_state *bsg_global_libunwind_state;

bool bsg_configure_libunwind(void) {
  bsg_global_libunwind_state = calloc(1, sizeof(bsg_libunwind_state));
  return true;
}

static _Unwind_Reason_Code
bsg_libunwind_callback(struct _Unwind_Context *context, void *arg) __asyncsafe {
  bsg_libunwind_state *state = (bsg_libunwind_state *)arg;
  uintptr_t ip = _Unwind_GetIP(context);

  if (state->frame_count >= BUGSNAG_FRAMES_MAX) {
    return _URC_END_OF_STACK;
  } else if (state->frame_count > 0 &&
             ip == state->frame_addresses[state->frame_count - 1]) { // already counted
    return _URC_NO_REASON;
  } else if (state->frame_count > 0 && (void *)ip == NULL) { // nobody's home
    return _URC_NO_REASON;
  }
  state->frame_addresses[state->frame_count] = ip;
  state->frame_count++;

  return _URC_NO_REASON;
}

ssize_t
bsg_unwind_stack_libunwind(bsg_stackframe stacktrace[BUGSNAG_FRAMES_MAX],
                           siginfo_t*_info, void *_user_context) {
  bsg_global_libunwind_state->frame_count = 0;
  _Unwind_Backtrace(bsg_libunwind_callback, bsg_global_libunwind_state);
  for (int i = 0; i < bsg_global_libunwind_state->frame_count; ++i) {
    stacktrace[i].frame_address =
        bsg_global_libunwind_state->frame_addresses[i];
  }
  return bsg_global_libunwind_state->frame_count;
}
