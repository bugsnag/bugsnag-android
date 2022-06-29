#include <malloc.h>

#include "memory.h"

#ifdef __cplusplus
extern "C" {
#endif

/**
 * Global shared context for Bugsnag reports
 */
static bsg_environment *bsg_global_env;

void bsg_init_memory(bsg_environment *env) { bsg_global_env = env; }

void bsg_free(void *ptr) {
  /*
   * free is only "active" when there are no signal handlers active, if there is
   * a crash in progress the memory cannot be safely released within the process
   * (free is not async safe), and the heap will be released along with the rest
   * of the process when the crash is over.
   */
  if (bsg_global_env->handling_crash) {
    return;
  }

  free(ptr);
}

#ifdef __cplusplus
}
#endif
