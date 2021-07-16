#include "buffered_writer.h"
#include "bugsnag_ndk.h"
#include <fcntl.h>
#include <inttypes.h>
#include <malloc.h>
#include <memory.h>
#include <unistd.h>

static bool bsg_buffered_writer_flush(struct bsg_buffered_writer *writer) {
  const int fd = writer->fd;
  const char *buff = writer->buffer;
  int bytes_to_write = writer->pos;

  if (bytes_to_write == 0) {
    return true;
  }

  // Try a few times, then give up.
  // write() will write less bytes on signal interruption, disk full, or
  // resource limit.
  for (int i = 0; i < 10; i++) {
    int written_count = write(fd, buff, bytes_to_write);
    if (written_count == bytes_to_write) {
      writer->pos = 0;
      return true;
    }
    if (written_count < 0) {
      return false;
    }
    buff += written_count;
    bytes_to_write -= written_count;
  }

  return false;
}

// The compiler keeps changing sizeof(size_t) on different platforms, which
// breaks printf().
#if __SIZEOF_SIZE_T__ == 8
#define PRIsize_t PRIu64
#else
#define PRIsize_t PRIu32
#endif

static bool bsg_buffered_writer_write(struct bsg_buffered_writer *writer,
                                      const char *data, size_t length) {
  if (length > writer->size) {
    BUGSNAG_LOG("Error: Attempted to write data size %" PRIsize_t
                " into buffer size %" PRIsize_t,
                length, writer->size);
    return false;
  }

  if (length > writer->size - writer->pos) {
    if (!writer->flush(writer)) {
      return false;
    }
  }

  memcpy(writer->buffer + writer->pos, data, length);
  writer->pos += length;
  return true;
}

static bool bsg_buffered_writer_close(bsg_buffered_writer *writer) {
  writer->flush(writer);
  if (close(writer->fd) < 0) {
    return false;
  }
  free(writer->buffer);
  free(writer->path);
  free(writer);
  return true;
}

bsg_buffered_writer *bsg_buffered_writer_open(size_t buffer_size,
                                              const char *path) {
  bsg_buffered_writer *writer = NULL;
  int fd = open(path, O_CREAT | O_TRUNC | O_WRONLY, 0600);
  if (fd < 0) {
    goto fail;
  }

  writer = calloc(1, sizeof(*writer));
  if (writer == NULL) {
    goto fail;
  }

  writer->buffer = calloc(1, buffer_size);
  if (writer->buffer == NULL) {
    goto fail;
  }
  writer->path = strdup(path);
  if (writer->path == NULL) {
    goto fail;
  }

  writer->fd = fd;
  writer->size = buffer_size;
  writer->write = bsg_buffered_writer_write;
  writer->flush = bsg_buffered_writer_flush;
  writer->dispose = bsg_buffered_writer_close;

  return writer;

fail:
  if (fd > 0) {
    close(fd);
  }
  if (writer != NULL) {
    if (writer->buffer != NULL) {
      free(writer->buffer);
    }
    if (writer->path != NULL) {
      free(writer->path);
    }
    free(writer);
  }
  return NULL;
}
