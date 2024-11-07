#include <dirent.h>
#include <fcntl.h>
#include <sys/syscall.h>
#include <unistd.h>

#include "string.h"
#include "threads.h"

/*
 * We read thread states by scanning the tid list in `/proc/self/task`. Each
 * subdirectory here represents a task (tid) within the current application, and
 * each of these contains a `stat` file with the information we require.
 *
 * This behaviour needs to be async / signal-safe, which blocks us from using
 * many of the standard libc filesystem functions. To work-around this we resort
 * to Linux syscalls for the directory listing.
 */

#define TASK_STAT_PATH_PREFIX "/proc/self/task/"
// we don't want the null-terminator in the length
#define TASK_STAT_PATH_PREFIX_LEN (sizeof(TASK_STAT_PATH_PREFIX) - 1)
#define TASK_STAT_PATH_SUFFIX "/stat"

// in theory we could optimise this - since all paths have a common prefix
static void path_for_tid_stat(char *dest, const char *tid) {
  size_t tidlen = bsg_strlen(tid);
  size_t remaining = MAX_STAT_PATH_LENGTH;
  bsg_strncpy(dest, TASK_STAT_PATH_PREFIX, remaining);
  remaining -= TASK_STAT_PATH_PREFIX_LEN;
  bsg_strncpy(&dest[TASK_STAT_PATH_PREFIX_LEN], tid, remaining);
  remaining -= tidlen;
  bsg_strncpy(&dest[TASK_STAT_PATH_PREFIX_LEN + tidlen], TASK_STAT_PATH_SUFFIX,
              remaining);
}

/**
 * Possible task states as defined in:
 * https://github.com/torvalds/linux/blob/v5.13/fs/proc/array.c#L132-L143
 */
static const char *const task_state_array[] = {
    "R running", "S sleeping", "D disk sleep", "T stopped", "t tracing stop",
    "X dead",    "Z zombie",   "P parked",     "I idle",
};

static void get_task_state_description(const char code, char *dest,
                                       size_t dest_size) {
  for (size_t index = 0; index < (sizeof(task_state_array) / sizeof(char *));
       index++) {
    if (task_state_array[index][0] == code) {
      bsg_strncpy(dest, &task_state_array[index][2], dest_size);
      return;
    }
  }

  // if we reached here we failed to map the code, store the single-character
  // code as a fallback
  dest[0] = code;
  dest[1] = '\0';
}

/**
 * Parses the content of a /proc/self/task/{tid}/stat file, as documented in:
 * https://man7.org/linux/man-pages/man5/proc.5.html
 *
 * We are only interested in the first three fields which are the:
 * 1) TID   (numeric)
 * 2) Name  (in parenthesis)
 * 3) State (single character)
 *
 * Unfortunately thread names can contain spaces, making tools like `strtok`
 * unsuitable for this process. As a result we parse using a simple single pass
 * state-based loop.
 *
 * *WARNING* `content` is modified by this method under normal operation
 *
 * @return true if the thread-data was parsed, false if not
 */
static bool parse_stat_content(bsg_thread *dest, char *content, size_t len) {
  enum stat_parse_state {
    PARSE_ID,
    PARSE_NAME_START,
    PARSE_NAME_CONTENT,
    PARSE_NAME_END,
    PARSE_STATUS,
    PARSE_DONE
  };

  enum stat_parse_state state = PARSE_ID;

  size_t i = 0;
  size_t name_length = 0;
  while (i < len) {
    char current = content[i];

    switch (state) {
    case PARSE_ID:
      if (current == ' ') {
        // we create a terminator for the numeric parse
        content[i] = '\0';
        // atoi is async-safe, strtol is not guaranteed to be
        dest->id = atoi(content);
        state = PARSE_NAME_START;
      }
      break;
    case PARSE_NAME_START:
      if (current == '(') {
        state = PARSE_NAME_CONTENT;
      }
      break;
    case PARSE_NAME_CONTENT:
      if (current == ')') {
        state = PARSE_NAME_END;
      } else if (name_length < sizeof(dest->name)) {
        dest->name[name_length] = current;
        name_length++;
      }
      break;
    case PARSE_NAME_END:
      dest->name[name_length] = '\0';
      if (current == ' ') {
        state = PARSE_STATUS;
      }
      break;
    case PARSE_STATUS:
      get_task_state_description(current, dest->state, sizeof(dest->state));
      state = PARSE_DONE;
      break;
    case PARSE_DONE:
      goto end;
    }

    i += 1;
  }

end:
  // success if we hit the DONE marker
  return state == PARSE_DONE;
}

/*
 * Reads the /stat file fields we are looking for (TID, Name, State) into `dest`
 * with a signal-safe approach.
 */
static bool read_thread_state(bsg_thread *dest, const char *tid) {
  // we filter out anything not numeric
  if (tid[0] < '0' || tid[0] > '9') {
    return false;
  }

  char filename[MAX_STAT_PATH_LENGTH];
  path_for_tid_stat(filename, tid);
  // the content buffer for the stat file data, in the format:
  // {tid} ({name}) {status}
  // {tid}    = integer TID value
  // {name}   = thread name char[16]
  // {status} = single character
  char content_buffer[64];
  int stat_fd = open(filename, O_RDONLY);

  if (stat_fd == 0) {
    return false;
  }

  size_t len = read(stat_fd, content_buffer, sizeof(content_buffer));
  bool parse_success = parse_stat_content(dest, content_buffer, len);
  close(stat_fd);
  return parse_success;
}

size_t bsg_capture_thread_states(pid_t reporting_tid, bsg_thread *threads,
                                 size_t max_threads) {
  size_t total_thread_count = 0;
  struct dirent64 *entry;
  char buffer[1024];
  int available, offset;

  int task_dir_fd = open("/proc/self/task", O_RDONLY | O_DIRECTORY);
  if (task_dir_fd == 0) {
    return 0;
  }

  while (total_thread_count < max_threads) {
    available = syscall(SYS_getdents64, task_dir_fd, buffer, sizeof(buffer));
    if (available <= 0) {
      break;
    }

    for (offset = 0; offset < available && total_thread_count < max_threads;) {
      entry = (struct dirent64 *)(buffer + offset);
      if (read_thread_state(&threads[total_thread_count], entry->d_name)) {
        threads[total_thread_count].is_reporting_thread =
            threads[total_thread_count].id == reporting_tid;
        total_thread_count += 1;
      }

      offset += entry->d_reclen;
    }
  }

  close(task_dir_fd);

  return total_thread_count;
}
