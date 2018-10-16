#include "../utils/build.h"
#include "stack_unwinder_libunwind.h"
#include <malloc.h>
#include <report.h>
#include <unwind.h>

typedef struct {
  size_t frame_count;
  uintptr_t frame_addresses[BUGSNAG_FRAMES_MAX];
#if defined(__arm__)
  void *signal_context;
#endif
  siginfo_t *signal_info;
} bsg_libunwind_state;

bsg_libunwind_state *bsg_global_libunwind_state;

bool bsg_configure_libunwind(void) {
  bsg_global_libunwind_state = calloc(1, sizeof(bsg_libunwind_state));
  return true;
}

static _Unwind_Reason_Code
bsg_libunwind_callback(struct _Unwind_Context *context, void *arg) __asyncsafe {
  bsg_libunwind_state *state = (bsg_libunwind_state *)arg;

#if defined(__arm__)
  if (state->frame_count == 0 && state->signal_context != NULL) {
    const ucontext_t *signal_ucontext =
        (const ucontext_t *)state->signal_context;
    const struct sigcontext *signal_mcontext = &(signal_ucontext->uc_mcontext);

    // Include program counter as the first frame
    state->frame_addresses[state->frame_count] = signal_mcontext->arm_pc;
    state->frame_count++;

    // Avoid unwinding in cases where there is a risk of segfault
    bool single_frame_mode = state->signal_info != NULL &&
                             (state->signal_info->si_code == SI_TKILL ||
                              state->signal_info->si_code == SEGV_MAPERR);
    if (single_frame_mode) {
      return _URC_END_OF_STACK;
    }

    /**
     * Set the registers and initial frame to the values from the signal
     * handler user context.
     *
     * When a signal is raised on 32-bit ARM, the current context is the signal
     * stack rather than the crash stack. To work around this, set the register
     * state before unwinding the first frame (using the program counter as the
     * first frame). Then the stack can be unwound normally.
     */
    _Unwind_SetGR(context, REG_R0, signal_mcontext->arm_r0);
    _Unwind_SetGR(context, REG_R1, signal_mcontext->arm_r1);
    _Unwind_SetGR(context, REG_R2, signal_mcontext->arm_r2);
    _Unwind_SetGR(context, REG_R3, signal_mcontext->arm_r3);
    _Unwind_SetGR(context, REG_R4, signal_mcontext->arm_r4);
    _Unwind_SetGR(context, REG_R5, signal_mcontext->arm_r5);
    _Unwind_SetGR(context, REG_R6, signal_mcontext->arm_r6);
    _Unwind_SetGR(context, REG_R7, signal_mcontext->arm_r7);
    _Unwind_SetGR(context, REG_R8, signal_mcontext->arm_r8);
    _Unwind_SetGR(context, REG_R9, signal_mcontext->arm_r9);
    _Unwind_SetGR(context, REG_R10, signal_mcontext->arm_r10);
    _Unwind_SetGR(context, REG_R11, signal_mcontext->arm_fp);
    _Unwind_SetGR(context, REG_R12, signal_mcontext->arm_ip);
    _Unwind_SetGR(context, REG_R13, signal_mcontext->arm_sp);
    _Unwind_SetGR(context, REG_R14, signal_mcontext->arm_lr);
    _Unwind_SetGR(context, REG_R15, signal_mcontext->arm_pc);
    return _URC_NO_REASON;
  }
#endif
  uintptr_t ip = _Unwind_GetIP(context);

  if (state->frame_count >= BUGSNAG_FRAMES_MAX) {
    return _URC_END_OF_STACK;
  } else if (state->frame_count > 0 && (void *)ip == NULL) { // nobody's home
    return _URC_NO_REASON;
  }
  state->frame_addresses[state->frame_count] = ip;
  state->frame_count++;

  return _URC_NO_REASON;
}

ssize_t
bsg_unwind_stack_libunwind(bsg_stackframe stacktrace[BUGSNAG_FRAMES_MAX],
                           siginfo_t *info, void *user_context) {
  bsg_global_libunwind_state->frame_count = 0;
#if defined(__arm__)
  bsg_global_libunwind_state->signal_context = user_context;
#endif
  bsg_global_libunwind_state->signal_info = info;
  // The return value of _Unwind_Backtrace sits on a throne of lies
  _Unwind_Backtrace(bsg_libunwind_callback, bsg_global_libunwind_state);
  for (int i = 0; i < bsg_global_libunwind_state->frame_count; ++i) {
    stacktrace[i].frame_address =
        bsg_global_libunwind_state->frame_addresses[i];
  }
  return bsg_global_libunwind_state->frame_count;
}
