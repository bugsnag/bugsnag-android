package com.bugsnag.android

import com.bugsnag.android.internal.MemoryMappedOutputStream
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.nio.BufferOverflowException

class MemoryMappedOutputStreamTest {
    @Rule @JvmField
    var folder: TemporaryFolder = TemporaryFolder()

    @Test
    fun testOpenClose() {
        val file = folder.newFile("stream.memmapped")
        val mm = MemoryMappedOutputStream(file, 4, clearedByteValue)
        mm.close()
        assertFileContents(file, byteArrayOf(0, 0, 0, 0))
    }

    @Test
    fun testClear() {
        val file = folder.newFile("stream.memmapped")
        val mm = MemoryMappedOutputStream(file, 10, clearedByteValue)
        mm.clear()
        mm.close()
        assertFileContents(file, byteArrayOf(0x11, 0x11, 0x11, 0x11, 0x11, 0x11, 0x11, 0x11, 0x11, 0x11))
    }

    @Test
    fun testFlush() {
        val file = folder.newFile("stream.memmapped")
        val mm = MemoryMappedOutputStream(file, 10, clearedByteValue)
        mm.clear()
        mm.write(0)
        mm.flush()
        assertFileContents(file, byteArrayOf(0x00, 0x11, 0x11, 0x11, 0x11, 0x11, 0x11, 0x11, 0x11, 0x11))
    }

    @Test
    fun testWriteByte() {
        val file = folder.newFile("stream.memmapped")
        val mm = MemoryMappedOutputStream(file, 10, clearedByteValue)
        mm.clear()
        mm.write(0)
        mm.close()
        assertFileContents(file, byteArrayOf(0x00, 0x11, 0x11, 0x11, 0x11, 0x11, 0x11, 0x11, 0x11, 0x11))
    }

    @Test
    fun testWriteBytes() {
        val file = folder.newFile("stream.memmapped")
        val mm = MemoryMappedOutputStream(file, 10, clearedByteValue)
        mm.clear()
        val bytes = byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12)
        mm.write(bytes, 0, 5)
        mm.close()
        assertFileContents(file, byteArrayOf(0x01, 0x02, 0x03, 0x04, 0x05, 0x11, 0x11, 0x11, 0x11, 0x11))
    }

    @Test
    fun testWriteBytesOffset() {
        val file = folder.newFile("stream.memmapped")
        val mm = MemoryMappedOutputStream(file, 10, clearedByteValue)
        mm.clear()
        val bytes = byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12)
        mm.write(bytes, 2, 5)
        mm.close()
        assertFileContents(file, byteArrayOf(0x03, 0x04, 0x05, 0x06, 0x07, 0x11, 0x11, 0x11, 0x11, 0x11))
    }

    @Test(expected = BufferOverflowException::class)
    fun testWriteOverflow() {
        val file = folder.newFile("stream.memmapped")
        val mm = MemoryMappedOutputStream(file, 10, clearedByteValue)
        mm.clear()
        val bytes = byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12)
        mm.write(bytes, 0, 11)
    }

    @Test(expected = BufferOverflowException::class)
    fun testWriteOverflowTwoSteps() {
        val file = folder.newFile("stream.memmapped")
        val mm = MemoryMappedOutputStream(file, 10, clearedByteValue)
        mm.clear()
        val bytes = byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12)
        mm.write(bytes, 0, 5)
        mm.write(bytes, 0, 6)
    }

    @Test
    fun testShrinkSize() {
        val file = folder.newFile("stream.memmapped")
        var mm = MemoryMappedOutputStream(file, 10, clearedByteValue)
        mm.clear()
        mm.write(0)
        mm.close()
        mm = MemoryMappedOutputStream(file, 5, clearedByteValue)
        mm.close()
        assertFileContents(file, byteArrayOf(0x00, 0x11, 0x11, 0x11, 0x11))
    }

    @Test
    fun testGrowSize() {
        val file = folder.newFile("stream.memmapped")
        var mm = MemoryMappedOutputStream(file, 5, clearedByteValue)
        mm.clear()
        mm.write(0)
        mm.close()
        mm = MemoryMappedOutputStream(file, 10, clearedByteValue)
        mm.close()
        assertFileContents(file, byteArrayOf(0x00, 0x11, 0x11, 0x11, 0x11, 0, 0, 0, 0, 0))
    }

    private fun assertFileContents(file: File, expectedContents: ByteArray) {
        val observedContents = file.readBytes()
        Assert.assertArrayEquals(expectedContents, observedContents)
    }

    // Can be any arbitrary value
    val clearedByteValue = 0x11.toByte()
}
