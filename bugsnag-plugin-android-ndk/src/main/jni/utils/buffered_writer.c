//
// Created by Jason Morris on 19/11/2021.
//

#include "buffered_writer.h"
#include "bugsnag_ndk.h"
#include <fcntl.h>
#include <inttypes.h>
#include <malloc.h>
#include <memory.h>
#include <unistd.h>

static bool bsg_flush_impl(const int fd, const char *buff,
                           size_t bytes_to_write) {
  if (bytes_to_write == 0) {
    return true;
  }

  // Try a few times, then give up.
  // write() will write less bytes on signal interruption, disk full, or
  // resource limit.
  for (int i = 0; i < 10; i++) {
    ssize_t written_count = write(fd, buff, bytes_to_write);
    if (written_count == bytes_to_write) {
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

static bool bsg_buffered_writer_flush(struct bsg_buffered_writer *writer) {
  if (bsg_flush_impl(writer->fd, writer->buffer, writer->pos)) {
    writer->pos = 0;
    return true;
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
                                      const void *data, size_t length) {

  if (length > BSG_BUFFER_SIZE) {
    // this data won't fit in the buffer, so we first flush anything already in
    // the buffer
    if (!writer->flush(writer)) {
      return false;
    }

    // then we flush the data we're needing to write
    if (!bsg_flush_impl(writer->fd, data, length)) {
      return false;
    }

    // then we return
    return true;
  }

  // the data will fit into the buffer, but the data doesn't have the space
  if (length > BSG_BUFFER_SIZE - writer->pos) {
    // so we flush the buffer first
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
  return true;
}

bool bsg_buffered_writer_open(struct bsg_buffered_writer *writer,
                              const char *path) {
  int fd = open(path, O_CREAT | O_TRUNC | O_WRONLY, 0600);
  if (fd < 0) {
    goto fail;
  }

  writer->fd = fd;
  writer->pos = 0;
  writer->write = bsg_buffered_writer_write;
  writer->flush = bsg_buffered_writer_flush;
  writer->dispose = bsg_buffered_writer_close;

  return true;

fail:
  if (fd > 0) {
    close(fd);
  }
  return false;
}