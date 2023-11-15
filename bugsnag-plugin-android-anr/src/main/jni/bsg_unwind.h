#ifndef BSG_UNWIND_H
#define BSG_UNWIND_H

#include "include/event.h"

// These are isolated because the ANR plugin symlinks to this file and to
// ../assets/include/event.h

#ifndef BUGSNAG_FRAMES_MAX
/**
 *  Number of frames in a stacktrace. Configures a default if not defined.
 */
#define BUGSNAG_FRAMES_MAX 192
#endif

#endif
