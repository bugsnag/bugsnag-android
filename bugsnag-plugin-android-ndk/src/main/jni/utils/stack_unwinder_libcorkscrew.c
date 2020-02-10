#include "stack_unwinder_libcorkscrew.h"
#include <dlfcn.h>
#include <event.h>
#include <signal.h>
#include <stdlib.h>
#include <unistd.h>

#include "string.h"

typedef struct {
  uintptr_t absolute_pc;
  uintptr_t stack_top;
  size_t stack_size;
} backtrace_frame_t;

typedef struct {
  uintptr_t relative_pc;
  uintptr_t relative_symbol_addr;
  char *map_name;
  char *symbol_name;
  char *demangled_name;
} backtrace_symbol_t;

/* Extracted from Android's include/corkscrew/backtrace.h */
typedef struct map_info_t map_info_t;

struct bsg_unwind_config {
  void *cork_unwind_backtrace_signal_arch;
  void *cork_unwind_backtrace_thread;
  void *cork_acquire_my_map_info_list;
  void *cork_release_my_map_info_list;
  void *cork_get_backtrace_symbols;
  void *cork_free_backtrace_symbols;
};

static struct bsg_unwind_config *bsg_global_unwind_cfg;

bool bsg_libcorkscrew_configured() {
  return bsg_global_unwind_cfg->cork_unwind_backtrace_signal_arch != NULL &&
         bsg_global_unwind_cfg->cork_unwind_backtrace_thread != NULL &&
         bsg_global_unwind_cfg->cork_acquire_my_map_info_list != NULL &&
         bsg_global_unwind_cfg->cork_release_my_map_info_list != NULL &&
         bsg_global_unwind_cfg->cork_get_backtrace_symbols != NULL &&
         bsg_global_unwind_cfg->cork_free_backtrace_symbols != NULL;
}

bool bsg_configure_libcorkscrew(void) {
  bsg_global_unwind_cfg = calloc(1, sizeof(struct bsg_unwind_config));
  void *libcorkscrew = dlopen("libcorkscrew.so", RTLD_LAZY | RTLD_LOCAL);
  if (libcorkscrew != NULL) {
    bsg_global_unwind_cfg->cork_unwind_backtrace_signal_arch =
        dlsym(libcorkscrew, "unwind_backtrace_signal_arch");
    bsg_global_unwind_cfg->cork_acquire_my_map_info_list =
        dlsym(libcorkscrew, "acquire_my_map_info_list");
    bsg_global_unwind_cfg->cork_release_my_map_info_list =
        dlsym(libcorkscrew, "release_my_map_info_list");
    bsg_global_unwind_cfg->cork_get_backtrace_symbols =
        dlsym(libcorkscrew, "get_backtrace_symbols");
    bsg_global_unwind_cfg->cork_free_backtrace_symbols =
        dlsym(libcorkscrew, "free_backtrace_symbols");
    bsg_global_unwind_cfg->cork_unwind_backtrace_thread =
        dlsym(libcorkscrew, "unwind_backtrace_thread");
  }

  return bsg_libcorkscrew_configured();
}

ssize_t
bsg_unwind_stack_libcorkscrew(bsg_stackframe_t stacktrace[BUGSNAG_FRAMES_MAX],
                              siginfo_t *info, void *user_context) {
  backtrace_frame_t frames[BUGSNAG_FRAMES_MAX];
  backtrace_symbol_t symbols[BUGSNAG_FRAMES_MAX];
  map_info_t *(*acquire_my_map_info_list)(void) =
      bsg_global_unwind_cfg->cork_acquire_my_map_info_list;
  ssize_t (*unwind_backtrace_signal_arch)(
      siginfo_t *, void *, const map_info_t *, backtrace_frame_t *, size_t,
      size_t) = bsg_global_unwind_cfg->cork_unwind_backtrace_signal_arch;
  ssize_t (*unwind_backtrace_thread)(pid_t, backtrace_frame_t *, size_t,
                                     size_t) =
      bsg_global_unwind_cfg->cork_unwind_backtrace_thread;
  void (*release_my_map_info_list)(map_info_t *) =
      bsg_global_unwind_cfg->cork_release_my_map_info_list;
  void (*get_backtrace_symbols)(const backtrace_frame_t *, size_t,
                                backtrace_symbol_t *) =
      bsg_global_unwind_cfg->cork_get_backtrace_symbols;
  void (*free_backtrace_symbols)(backtrace_symbol_t *, size_t) =
      bsg_global_unwind_cfg->cork_free_backtrace_symbols;

  ssize_t size;
  if (user_context != NULL) {
    map_info_t *const info_list = acquire_my_map_info_list();
    size = unwind_backtrace_signal_arch(info, user_context, info_list, frames,
                                        0, (size_t)BUGSNAG_FRAMES_MAX);
    release_my_map_info_list(info_list);
  } else {
    size = unwind_backtrace_thread(getpid(), frames, 0,
                                   (size_t)BUGSNAG_FRAMES_MAX);
  }

  get_backtrace_symbols(frames, (size_t)size, symbols);
  int frame_count = 0;
  for (int i = 0; i < size; i++) {
    backtrace_frame_t backtrace_frame = frames[i];
    backtrace_symbol_t backtrace_symbol = symbols[i];

    if ((void *)backtrace_frame.absolute_pc == NULL) {
      continue; // nobody's home
    }
    if (frame_count > 0 && backtrace_frame.absolute_pc ==
                               stacktrace[frame_count - 1].frame_address) {
      continue; // already seen this
    }
    if (backtrace_symbol.symbol_name != NULL) {
      bsg_strcpy(stacktrace[frame_count].method, backtrace_symbol.symbol_name);
    }

    stacktrace[frame_count].frame_address = backtrace_frame.absolute_pc;
    frame_count++;
  }
  free_backtrace_symbols(symbols, (size_t)size);

  return frame_count;
}
