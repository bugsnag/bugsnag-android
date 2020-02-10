#include "string.h"
#include "stack_unwinder_libunwindstack.h"
#include <stdlib.h>
#include <ucontext.h>
#include <unwindstack/Elf.h>
#include <unwindstack/MapInfo.h>
#include <unwindstack/Maps.h>
#include <unwindstack/Memory.h>
#include <unwindstack/Regs.h>

ssize_t
bsg_unwind_stack_libunwindstack(bsg_stackframe_t stacktrace[BUGSNAG_FRAMES_MAX],
                                siginfo_t *info, void *user_context) {
  if (user_context == NULL) {
    return 0; // only handle unwinding from signals
  }

  // Fetch register values from signal context. To get registers without
  // a context, use unwindstack::Regs::CreateFromLocal()
  const std::unique_ptr<unwindstack::Regs> regs(
      unwindstack::Regs::CreateFromUcontext(unwindstack::Regs::CurrentArch(),
                                            user_context));

  std::string unw_function_name;
  unwindstack::LocalMaps maps;

  if (!maps.Parse()) {
    stacktrace[0].frame_address = regs->pc(); // only known frame
    return 1;
  }

  const std::shared_ptr<unwindstack::Memory> memory(
      new unwindstack::MemoryLocal);

  int frame_count = 0;
  for (int i = 0; i < BUGSNAG_FRAMES_MAX; i++) {
    stacktrace[frame_count++].frame_address = regs->pc();
    unwindstack::MapInfo *const map_info = maps.Find(regs->pc());
    if (!map_info) {
      break;
    }
    unwindstack::Elf *const elf = map_info->GetElf(memory, false);
    if (!elf) {
      break;
    }

    // Getting value of program counter relative module where a function is
    // located.
    const uint64_t rel_pc = elf->GetRelPc(regs->pc(), map_info);
    uint64_t adjusted_rel_pc = rel_pc;
    if (frame_count != 0) {
      // If it's not a first frame we need to rewind program counter value to
      // previous instruction. For the first frame pc from ucontext points
      // exactly to a failed instruction, for other frames rel_pc will contain
      // return address after function call instruction.
      adjusted_rel_pc -= regs->GetPcAdjustment(rel_pc, elf);
    }
    bool finished = false;
    if (!elf->Step(rel_pc, adjusted_rel_pc, map_info->elf_offset, regs.get(),
                   memory.get(), &finished)) {
      break;
    }
  }
  return frame_count;
}
