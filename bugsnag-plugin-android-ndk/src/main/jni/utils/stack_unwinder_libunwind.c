#include "build.h"
#include "stack_unwinder_libunwind.h"
#include <malloc.h>
#include <event.h>
#include <unwind.h>

#if defined(__arm__)
#include <libunwind.h>
#endif

typedef struct {
  size_t frame_count;
  uintptr_t frame_addresses[BUGSNAG_FRAMES_MAX];
} bsg_libunwind_state;

bsg_libunwind_state *bsg_global_libunwind_state;
bool bsg_libunwind_global_is32bit = false;

bool bsg_configure_libunwind(bool is32bit) {
  bsg_global_libunwind_state = calloc(1, sizeof(bsg_libunwind_state));
  bsg_libunwind_global_is32bit = is32bit;
  return true;
}

static _Unwind_Reason_Code
bsg_libunwind_callback(struct _Unwind_Context *context, void *arg) __asyncsafe {
  bsg_libunwind_state *state = (bsg_libunwind_state *)arg;

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

#if defined(__arm__)
ssize_t
bsg_unwind_stack_libunwind_arm32(bsg_stackframe_t stacktrace[BUGSNAG_FRAMES_MAX],
                                 siginfo_t *info, void *user_context) __asyncsafe {
  unw_cursor_t cursor;
  unw_context_t uc;
  int index = 0;

  unw_getcontext(&uc);
  unw_init_local(&cursor, &uc);
  // Initialize cursor state with register data, if any
  if (user_context != NULL) {
    /**
     * Set the registers and initial frame to the values from the signal
     * handler user context.
     *
     * When a signal is raised on 32-bit ARM, the current context is the signal
     * stack rather than the crash stack. To work around this, set the register
     * state before unwinding the first frame (using the program counter as the
     * first frame). Then the stack can be unwound normally.
     */
    const ucontext_t *signal_ucontext = (const ucontext_t *)user_context;
    const struct sigcontext *signal_mcontext = &(signal_ucontext->uc_mcontext);
    unw_set_reg(&cursor, UNW_ARM_R0, signal_mcontext->arm_r0);
    unw_set_reg(&cursor, UNW_ARM_R1, signal_mcontext->arm_r1);
    unw_set_reg(&cursor, UNW_ARM_R2, signal_mcontext->arm_r2);
    unw_set_reg(&cursor, UNW_ARM_R3, signal_mcontext->arm_r3);
    unw_set_reg(&cursor, UNW_ARM_R4, signal_mcontext->arm_r4);
    unw_set_reg(&cursor, UNW_ARM_R5, signal_mcontext->arm_r5);
    unw_set_reg(&cursor, UNW_ARM_R6, signal_mcontext->arm_r6);
    unw_set_reg(&cursor, UNW_ARM_R7, signal_mcontext->arm_r7);
    unw_set_reg(&cursor, UNW_ARM_R8, signal_mcontext->arm_r8);
    unw_set_reg(&cursor, UNW_ARM_R9, signal_mcontext->arm_r9);
    unw_set_reg(&cursor, UNW_ARM_R10, signal_mcontext->arm_r10);
    unw_set_reg(&cursor, UNW_ARM_R11, signal_mcontext->arm_fp);
    unw_set_reg(&cursor, UNW_ARM_R12, signal_mcontext->arm_ip);
    unw_set_reg(&cursor, UNW_ARM_R13, signal_mcontext->arm_sp);
    unw_set_reg(&cursor, UNW_ARM_R14, signal_mcontext->arm_lr);
    unw_set_reg(&cursor, UNW_ARM_R15, signal_mcontext->arm_pc);
    unw_set_reg(&cursor, UNW_REG_IP, signal_mcontext->arm_pc);
    unw_set_reg(&cursor, UNW_REG_SP, signal_mcontext->arm_sp);
    // Manually insert first frame to avoid being skipped in step()
    stacktrace[index++].frame_address = signal_mcontext->arm_pc;
  }

  while (unw_step(&cursor) > 0 && index < BUGSNAG_FRAMES_MAX) {
    unw_word_t ip = 0;
    unw_get_reg(&cursor, UNW_REG_IP, &ip);
    stacktrace[index++].frame_address = ip;
  }

  return index;
}
#endif
ssize_t
bsg_unwind_stack_libunwind(bsg_stackframe_t stacktrace[BUGSNAG_FRAMES_MAX],
                           siginfo_t *info, void *user_context) {
#if defined(__arm__)
  if (bsg_libunwind_global_is32bit) { // avoid this code path if a 64-bit device
                                      // is running 32-bit
    return bsg_unwind_stack_libunwind_arm32(stacktrace, info, user_context);
  }
#endif
  if (bsg_global_libunwind_state == NULL) {
    return 0;
  }
  bsg_global_libunwind_state->frame_count = 0;
  // The return value of _Unwind_Backtrace sits on a throne of lies
  _Unwind_Backtrace(bsg_libunwind_callback, bsg_global_libunwind_state);
  for (int i = 0; i < bsg_global_libunwind_state->frame_count; ++i) {
    stacktrace[i].frame_address =
        bsg_global_libunwind_state->frame_addresses[i];
  }
  return bsg_global_libunwind_state->frame_count;
}
