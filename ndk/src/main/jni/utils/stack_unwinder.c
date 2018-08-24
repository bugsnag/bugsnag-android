#include "stack_unwinder.h"
#include "stack_unwinder_libcorkscrew.h"
#include "stack_unwinder_libunwind.h"
#include "string.h"
#include <dlfcn.h>
#include <report.h>

#define BSG_LIBUNWIND_LEVEL 21
#define BSG_LIBCORKSCREW_MIN_LEVEL 16
#define BSG_LIBCORKSCREW_MAX_LEVEL 19

bsg_unwinder bsg_get_unwind_type(int apiLevel) {
  if (apiLevel >= BSG_LIBUNWIND_LEVEL && bsg_configure_libunwind()) {
    return BSG_LIBUNWIND;
  } else if (apiLevel <= BSG_LIBCORKSCREW_MAX_LEVEL &&
             apiLevel >= BSG_LIBCORKSCREW_MIN_LEVEL &&
             bsg_configure_libcorkscrew()) {
    return BSG_LIBCORKSCREW;
  }
  return BSG_CUSTOM_UNWIND;
}

void bsg_insert_fileinfo(ssize_t frame_count,
                         bsg_stackframe stacktrace[BUGSNAG_FRAMES_MAX]) {
  static Dl_info info;
  for (int i = 0; i < frame_count; ++i) {
    if (dladdr((void *)stacktrace[i].frame_address, &info) != 0 &&
        info.dli_fname != NULL) {
      bsg_strcpy(stacktrace[i].filename, (char *)info.dli_fname);
    }
  }
}

ssize_t bsg_unwind_stack(bsg_unwinder unwind_style,
                         bsg_stackframe stacktrace[BUGSNAG_FRAMES_MAX],
                         siginfo_t *info, void *user_context) {
  ssize_t frame_count = 0;
  if (unwind_style == BSG_LIBUNWIND) {
    frame_count = bsg_unwind_stack_libunwind(stacktrace, info, user_context);
  } else if (unwind_style == BSG_LIBCORKSCREW) {
    frame_count = bsg_unwind_stack_libcorkscrew(stacktrace, info, user_context);
  } else {
    stacktrace[0].frame_address = (uintptr_t)info->si_addr;
    frame_count = 1;
  }
  bsg_insert_fileinfo(frame_count,
                      stacktrace); // none of this is safe ¯\_(ツ)_/¯

  return frame_count;
}
