/**
 * Async-safe buffered writer.
 *
 * This writer buffers up data and flushes to disk as needed, using primitive
 * async-safe functions open(), close(), and write() under the hood.
 */
#ifndef BUGSNAG_BUFFERED_WRITER_H
#define BUGSNAG_BUFFERED_WRITER_H

#include <stdbool.h>
#include <stdint.h>

typedef struct bsg_buffered_writer {
  int fd;
  size_t size;
  size_t pos;
  char *path;
  char *buffer;

  /**
   * Write to this writer. If the internal buffer size is exceeded, it will
   * automatically flush to file.
   *
   * Note: This method is async-safe.
   *
   * @param writer This writer.
   * @param data The data to write.
   * @param length The length of the data to write.
   * @return True on success. Check errno on error.
   */
  bool (*write)(struct bsg_buffered_writer *writer, const char *data,
                size_t length);

  /**
   * Force a flush to file.
   *
   * Note: This method is async-safe.
   *
   * @param writer This writer.
   * @return True on success. Check errno on error.
   */
  bool (*flush)(struct bsg_buffered_writer *writer);

  /**
   * Dispose of this writer, closing and freeing all resources. The pointer to
   * writer will be invalid after this call.
   *
   * Note: This method is NOT async-safe!
   *
   * @param writer This writer.
   * @return True on success. Check errno on error.
   */
  bool (*dispose)(struct bsg_buffered_writer *writer);
} bsg_buffered_writer;

/**
 * Create a new buffered writer.
 *
 * Note: This function is NOT async-safe!
 *
 * @param buffer_size The size of the buffer to use.
 * @param path The path of the file to write to.
 * @return A buffered writer, or NULL on error. Check errno on error.
 */
bsg_buffered_writer *bsg_buffered_writer_open(size_t buffer_size,
                                              const char *path);

#endif
