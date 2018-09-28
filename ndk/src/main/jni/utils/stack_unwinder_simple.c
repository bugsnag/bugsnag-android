#include "../utils/string.h"
#include "stack_unwinder_simple.h"
#include <stdlib.h>
#include <ucontext.h>

ssize_t bsg_unwind_stack_simple(bsg_stackframe stacktrace[BUGSNAG_FRAMES_MAX],
                                siginfo_t *info, void *user_context) {
  ssize_t frame_count = 0;
  if (user_context != NULL) {
    // program counter / instruction pointer
    uintptr_t ip = 0;
    ucontext_t *ctx = (ucontext_t *)user_context;
#if defined(__i386__)
    ip = (uintptr_t)ctx->uc_mcontext.gregs[REG_EIP];
#elif defined(__x86_64__)
    ip = (uintptr_t)ctx->uc_mcontext.gregs[REG_RIP];
#elif defined(__arm__)
    ip = (uintptr_t)ctx->uc_mcontext.arm_ip;
#elif defined(__aarch64__)
    ip = (uintptr_t)ctx->uc_mcontext.regs[15];
#else
    ip = (uintptr_t)info->si_addr;
#endif
    if (ip != 0) {
      stacktrace[frame_count++].frame_address = ip;
      frame_count = 1;
    }
  }

  return frame_count;
}
