#include "stack_unwinder.h"
#include "stack_unwinder_libcorkscrew.h"
#include "stack_unwinder_libunwind.h"
#include "stack_unwinder_libunwindstack.h"
#include "stack_unwinder_simple.h"
#include "string.h"
#include <asm/siginfo.h>
#include <dlfcn.h>
#include <event.h>
#include <ucontext.h>

#define BSG_LIBUNWIND_LEVEL 21
#define BSG_LIBUNWINDSTACK_LEVEL 15
#define BSG_LIBUNWIND_LEVEL_ARM32 16
#define BSG_LIBCORKSCREW_MIN_LEVEL 16
#define BSG_LIBCORKSCREW_MAX_LEVEL 19

void bsg_set_unwind_types(int apiLevel, bool is32bit, bsg_unwinder *signal_type,
                          bsg_unwinder *other_type) {
#if defined(__arm__)
  if (apiLevel >= BSG_LIBUNWIND_LEVEL_ARM32 && is32bit &&
      bsg_configure_libunwind(is32bit)) {
    if (apiLevel >= BSG_LIBUNWIND_LEVEL) {
      *signal_type = BSG_LIBUNWIND;
    } else if (bsg_configure_libcorkscrew()) {
      *signal_type = BSG_LIBCORKSCREW;
    } else {
      *signal_type = BSG_CUSTOM_UNWIND;
    }
    *other_type = BSG_LIBUNWIND;
    return;
  }
#endif
  if (apiLevel >= BSG_LIBUNWINDSTACK_LEVEL) {
    bsg_configure_libunwind(is32bit);
    *signal_type = BSG_LIBUNWINDSTACK;
    *other_type = BSG_LIBUNWIND;
  } else {
    *signal_type = BSG_CUSTOM_UNWIND;
    *other_type = BSG_CUSTOM_UNWIND;
  }
}

void bsg_insert_fileinfo(ssize_t frame_count,
                         bsg_stackframe_t stacktrace[BUGSNAG_FRAMES_MAX]) {
  static Dl_info info;
  for (int i = 0; i < frame_count; ++i) {
    if (dladdr((void *)stacktrace[i].frame_address, &info) != 0) {
      stacktrace[i].load_address = (uintptr_t)info.dli_fbase;
      stacktrace[i].symbol_address = (uintptr_t)info.dli_saddr;
      stacktrace[i].line_number =
          stacktrace[i].frame_address - stacktrace[i].load_address;
      if (info.dli_fname != NULL) {
        bsg_strcpy(stacktrace[i].filename, (char *)info.dli_fname);
      }
      if (info.dli_sname != NULL) {
        bsg_strcpy(stacktrace[i].method, (char *)info.dli_sname);
      }
    }
  }
}

ssize_t bsg_unwind_stack(bsg_unwinder unwind_style,
                         bsg_stackframe_t stacktrace[BUGSNAG_FRAMES_MAX],
                         siginfo_t *info, void *user_context) {
  ssize_t frame_count = 0;
  if (unwind_style == BSG_LIBUNWINDSTACK) {
    frame_count =
        bsg_unwind_stack_libunwindstack(stacktrace, info, user_context);
  } else if (unwind_style == BSG_LIBUNWIND) {
    frame_count = bsg_unwind_stack_libunwind(stacktrace, info, user_context);
  } else if (unwind_style == BSG_LIBCORKSCREW) {
    frame_count = bsg_unwind_stack_libcorkscrew(stacktrace, info, user_context);
  } else {
    frame_count = bsg_unwind_stack_simple(stacktrace, info, user_context);
  }
  bsg_insert_fileinfo(frame_count,
                      stacktrace); // none of this is safe ¯\_(ツ)_/¯

  return frame_count;
}
