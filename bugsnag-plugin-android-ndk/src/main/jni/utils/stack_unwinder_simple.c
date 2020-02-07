#include "string.h"
#include "stack_unwinder_simple.h"
#include <stdlib.h>
#include <ucontext.h>

ssize_t bsg_unwind_stack_simple(bsg_stackframe_t stacktrace[BUGSNAG_FRAMES_MAX],
                                siginfo_t *info, void *user_context) {
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
      stacktrace[0].frame_address = ip;
      return 1;
    }
  }

  return 0;
}
