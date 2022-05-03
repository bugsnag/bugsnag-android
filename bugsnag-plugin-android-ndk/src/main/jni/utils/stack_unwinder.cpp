#include "stack_unwinder.h"

#include "string.h"

#include <dlfcn.h>
#include <unwindstack/LocalUnwinder.h>
#include <unwindstack/Maps.h>
#include <unwindstack/MemoryLocal.h>
#include <unwindstack/Regs.h>
#include <unwindstack/RegsGetLocal.h>
#include <unwindstack/Unwinder.h>

// unwinder intended for a potentially terminating context
static unwindstack::Unwinder *crash_time_unwinder;
// soft lock for using the crash time unwinder - if active, return without
// attempting to unwind. This isn't a "real" lock to avoid deadlocking in the
// event of a crash while handling an ANR or the reverse.
static bool unwinding_crash_stack;

// Thread-safe, reusable unwinder - uses thread-specific memory caches
static unwindstack::LocalUnwinder *current_time_unwinder;

static bool attempted_init;

void bsg_unwinder_init() {
  if (attempted_init) {
    // already initialized or failed to init, cannot be done more than once
    return;
  }
  attempted_init = true;

  auto crash_time_maps = new unwindstack::LocalMaps();
  if (crash_time_maps->Parse()) {
    std::shared_ptr<unwindstack::Memory> crash_time_memory(
        new unwindstack::MemoryLocal);
    crash_time_unwinder = new unwindstack::Unwinder(
        BUGSNAG_FRAMES_MAX, crash_time_maps,
        unwindstack::Regs::CreateFromLocal(), crash_time_memory);
    auto arch = unwindstack::Regs::CurrentArch();
    auto dexfiles_ptr = unwindstack::CreateDexFiles(arch, crash_time_memory);
    crash_time_unwinder->SetDexFiles(dexfiles_ptr.get());
  }

  current_time_unwinder = new unwindstack::LocalUnwinder();
  if (!current_time_unwinder->Init()) {
    delete current_time_unwinder;
    current_time_unwinder = nullptr;
  }
}

ssize_t bsg_unwind_crash_stack(bugsnag_stackframe stack[BUGSNAG_FRAMES_MAX],
                               siginfo_t *info, void *user_context) {
  if (crash_time_unwinder == nullptr || unwinding_crash_stack) {
    return 0;
  }
  unwinding_crash_stack = true;
  if (user_context) {
    crash_time_unwinder->SetRegs(unwindstack::Regs::CreateFromUcontext(
        unwindstack::Regs::CurrentArch(), user_context));
  } else {
    auto regs = unwindstack::Regs::CreateFromLocal();
    unwindstack::RegsGetLocal(regs);
    crash_time_unwinder->SetRegs(regs);
  }

  crash_time_unwinder->Unwind();
  int frame_count = 0;
  for (auto &frame : crash_time_unwinder->frames()) {
    stack[frame_count].frame_address = frame.pc;
    stack[frame_count].line_number = frame.rel_pc;
    stack[frame_count].load_address = frame.map_start;
    stack[frame_count].symbol_address = frame.pc - frame.function_offset;
    bsg_strncpy(stack[frame_count].filename, frame.map_name.c_str(),
                sizeof(stack[frame_count].filename));
    bsg_strncpy(stack[frame_count].method, frame.function_name.c_str(),
                sizeof(stack[frame_count].method));

    // if the filename or method name cannot be found - try fallback to dladdr
    // to find them
    static Dl_info info;
    if ((frame.map_name.empty() || frame.function_name.empty()) &&
        dladdr((void *)frame.pc, &info) != 0) {

      if (info.dli_fname != nullptr) {
        bsg_strncpy(stack[frame_count].filename, (char *)info.dli_fname,
                    sizeof(stack[frame_count].filename));
      }
      if (info.dli_sname != nullptr) {
        bsg_strncpy(stack[frame_count].method, (char *)info.dli_sname,
                    sizeof(stack[frame_count].method));
      }
    }

    frame_count++;
  }
  unwinding_crash_stack = false;
  return frame_count;
}

ssize_t
bsg_unwind_concurrent_stack(bugsnag_stackframe stack[BUGSNAG_FRAMES_MAX],
                            siginfo_t *_info, void *_context) {
  if (current_time_unwinder == nullptr) {
    return 0;
  }

  std::vector<unwindstack::LocalFrameData> frames;
  current_time_unwinder->Unwind(&frames, BUGSNAG_FRAMES_MAX);
  int frame_count = 0;
  for (auto &frame : frames) {
    stack[frame_count].frame_address = frame.pc;
    if (frame.map_info != nullptr) {
      stack[frame_count].line_number = frame.rel_pc;
      stack[frame_count].load_address = frame.map_info->start();
      stack[frame_count].symbol_address = frame.pc - frame.map_info->offset();
      bsg_strncpy(stack[frame_count].filename, frame.map_info->name().c_str(),
                  sizeof(stack[frame_count].filename));

      // if the filename or method name cannot be found - try fallback to dladdr
      // to find them
      static Dl_info info;
      if ((frame.map_info->name().empty() || frame.function_name.empty()) &&
          dladdr((void *)frame.pc, &info) != 0) {

        if (info.dli_fname != nullptr) {
          bsg_strncpy(stack[frame_count].filename, (char *)info.dli_fname,
                      sizeof(stack[frame_count].filename));
        }
        if (info.dli_sname != nullptr) {
          bsg_strncpy(stack[frame_count].method, (char *)info.dli_sname,
                      sizeof(stack[frame_count].method));
        }
      }
    }
    bsg_strncpy(stack[frame_count].method, frame.function_name.c_str(),
                sizeof(stack[frame_count].method));
    frame_count++;
  }
  return frame_count;
}
