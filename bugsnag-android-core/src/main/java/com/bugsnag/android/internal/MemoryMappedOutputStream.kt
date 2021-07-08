package com.bugsnag.android.internal

import java.io.File
import java.io.OutputStream
import java.io.RandomAccessFile
import java.nio.BufferOverflowException
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

/**
 * OutputStream wrapper around a shared memory file.
 *
 * Memory-mapped files are maintained by the operating system, and so they are guaranteed to match
 * the last data written to the memory region, even if the app crashes.
 *
 * The memory mapped file is fixed to the length specified at instantiation, meaning that
 * write() will throw java.nio.BufferOverflowException if the file is too full.
 */
class MemoryMappedOutputStream(
    file: File,
    private val bufferSize: Long,
    private val clearedByteValue: Byte = 0
) : OutputStream() {

    private val raf: RandomAccessFile = RandomAccessFile(file, "rw")
    private val memory: MappedByteBuffer =
        raf.channel.map(FileChannel.MapMode.READ_WRITE, 0, bufferSize)

    init {
        raf.setLength(bufferSize)
    }

    fun clear() {
        memory.rewind()
        for (i in 1..bufferSize) {
            memory.put(clearedByteValue)
        }
        memory.rewind()
    }

    override fun close() {
        memory.force()
        raf.close()
    }

    /**
     * Write a byte value to the file.
     * @throws java.nio.BufferOverflowException if the file is too full.
     */
    @Throws(BufferOverflowException::class)
    override fun write(i: Int) {
        memory.put(i.toByte())
    }

    /**
     * Write a byte array to the file.
     * @throws java.nio.BufferOverflowException if the file is too full.
     */
    @Throws(BufferOverflowException::class)
    override fun write(b: ByteArray, off: Int, len: Int) {
        memory.put(b, off, len)
    }
}
