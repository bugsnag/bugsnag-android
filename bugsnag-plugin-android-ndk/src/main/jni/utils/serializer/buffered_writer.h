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

#define BSG_BUFFER_SIZE 128

#ifdef __cplusplus
extern "C" {
#endif

typedef struct bsg_buffered_writer {
  int fd;
  size_t pos;
  char buffer[BSG_BUFFER_SIZE];

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
  bool (*write)(struct bsg_buffered_writer *writer, const void *data,
                size_t length);

  /**
   * Write a single byte-value to this writer.
   *
   * @param writer This writer
   * @param byte the single byte to be written
   * @return True on success. Check errno on error.
   */
  bool (*write_byte)(struct bsg_buffered_writer *writer, const uint8_t byte);

  /**
   * Write a length-prefixed string to this writer. This will first write 4
   * bytes for the length of the string, and then the string itself (without
   * it's null-terminator character).
   *
   * @param writer This writer
   * @param string the string to write, may not be NULL
   * @return True on success. Check errno on error.
   */
  bool (*write_string)(struct bsg_buffered_writer *writer, const char *string);

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
   * Note: This method is async-safe!
   *
   * @param writer This writer.
   * @return True on success. Check errno on error.
   */
  bool (*dispose)(struct bsg_buffered_writer *writer);
} bsg_buffered_writer;

/**
 * Create a new buffered writer.
 *
 * @param buffer_size The size of the buffer to use.
 * @param path The path of the file to write to.
 * @return A buffered writer, or NULL on error. Check errno on error.
 */
bool bsg_buffered_writer_open(struct bsg_buffered_writer *writer,
                              const char *path);

#ifdef __cplusplus
}
#endif

#endif