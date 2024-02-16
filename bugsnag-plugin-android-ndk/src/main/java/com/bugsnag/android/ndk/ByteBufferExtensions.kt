@file:Suppress("MagicNumber") // this file is filled with numbers used in modified-utf8
package com.bugsnag.android.ndk

import java.nio.ByteBuffer
import kotlin.math.min

private const val UTF_REPLACEMENT_CHAR = '\uFFFD'

/**
 * Read a value of the C type `int`, not to be confused with `getInt` which will always read
 *  * a Java `int`.
 */
internal fun ByteBuffer.getNativeInt(): Int = getInt()

/**
 * Read a value of the C type `long`, not to be confused with `getLong` which will always read
 * a Java `long`.
 */
internal fun ByteBuffer.getNativeLong(): Long =
    if (NativeArch.is32bit) getInt().toLong() else getLong()

/**
 * Read a value of type `time_t`, the resolution of the value returned *is not defined*. Some of
 * our properties are in seconds, others are in milliseconds.
 */
internal fun ByteBuffer.getNativeTime(): Long =
    if (NativeArch.is32bit) getInt().toLong() else getLong()

/**
 * Read a value of type `size_t`
 */
internal fun ByteBuffer.getNativeSize(): Long = getNativeLong()

/**
 * Read a value of type `bool`
 */
internal fun ByteBuffer.getNativeBool(): Boolean = get().toInt() and 0xff != 0

// -------------------------------------------------------------------------------------------------
// Read functions for fixed-width primitives
// -------------------------------------------------------------------------------------------------

internal fun ByteBuffer.getULong(): ULong = getLong().toULong()
internal fun ByteBuffer.getUInt(): UInt = getInt().toUInt()

/**
 * Decode [allocatedByteCount] as a null-terminated sequence of modified UTF-8 bytes. This reads
 * the same format as the JNI `NewUTFStringUTF` function, but also obeys a null-terminator character
 * used in C. This function will always consume *exactly* [allocatedByteCount] from this
 * `ByteBuffer`, but may return a `String` of fewer (or event zero) characters. This function
 * will always return a `String` and invalid UTF-8 sequences will cause the function to return
 * what has been successfully decoded up to that point.
 */
internal fun ByteBuffer.getCString(allocatedByteCount: Int): String {
    val origin = position()
    val maxBytes = min(allocatedByteCount, remaining())

    // allocate a CharArray to handle the decoded string
    // it can't be longer than the number of bytes in the buffer
    val chars = CharArray(maxBytes)
    var bytesRead = 0
    var outIndex = 0
    var c = 0

    // fast path for ASCII-7 compatible characters / strings
    while (bytesRead < maxBytes) {
        c = get(origin + bytesRead).toInt() and 0xff
        // 128+ = we need to take the "slow" path
        // 0 = null-terminator - this is the end of the string
        if (c >= 128 || c == 0) break

        chars[outIndex++] = c.toChar()
        bytesRead++
    }

    // make sure we didn't previously reach the end of the string
    if (c != 0) {
        outIndex = readModifiedUtf8(bytesRead, maxBytes, origin, chars, outIndex)
    }

    // move the ByteBuffer position to after the string
    position(origin + maxBytes)
    return String(chars, 0, outIndex)
}

/**
 * Read a modified-utf8 string directly from a `ByteBuffer`, this follows the same implementation
 * as [java.io.DataInputStream] but also covers an early-exit on null (zero) bytes, staying
 * compliant with the C-string format.
 *
 * @param bytesRead how many bytes have already been read by [getCString]
 * @param maxBytes the maximum number of bytes to read for this string
 * @param origin the position/index in the ByteBuffer of the first byte for this string,
 *               this is *not* the first byte to be read by this function
 * @param outBuffer the buffer to output the decoded characters into
 * @param outIndex the index within [outBuffer] of the first character to decode
 *
 * @return the length of the string that was decoded
 */
@Suppress("LoopWithTooManyJumpStatements", "CyclomaticComplexMethod")
private fun ByteBuffer.readModifiedUtf8(
    bytesRead: Int,
    maxBytes: Int,
    origin: Int,
    outBuffer: CharArray,
    outIndex: Int
): Int {
    var bytesRead1 = bytesRead
    var c: Int
    var outIndex1 = outIndex
    while (bytesRead1 < maxBytes) {
        c = get(origin + bytesRead1).toInt() and 0xff
        if (c == 0) {
            // null-terminator - this is the end of the string
            break
        }

        when (c shr 4) {
            0, 1, 2, 3, 4, 5, 6, 7 -> {
                /* 0xxxxxxx*/
                bytesRead1++
                outBuffer[outIndex1++] = c.toChar()
            }

            12, 13 -> {
                /* 110x xxxx   10xx xxxx*/
                bytesRead1 += 2
                if (bytesRead1 > maxBytes) {
                    // Invalid UTF-8 - but we don't error out, we return what we *do* have
                    outBuffer[outIndex1++] = UTF_REPLACEMENT_CHAR
                    break
                }

                val char2 = get(origin + bytesRead1 - 1).toInt() and 0xff
                if (char2 and 0xc0 != 0x80) {
                    // Invalid UTF-8 - but we don't error out, we return what we *do* have
                    outBuffer[outIndex1++] = UTF_REPLACEMENT_CHAR
                }

                outBuffer[outIndex1++] = ((c and 0x1f shl 6) or (char2 and 0x3f)).toChar()
            }

            14 -> {
                /* 1110 xxxx  10xx xxxx  10xx xxxx */
                bytesRead1 += 3
                if (bytesRead1 > maxBytes) {
                    // Invalid UTF-8 - but we don't error out, we return what we *do* have
                    outBuffer[outIndex1++] = UTF_REPLACEMENT_CHAR
                    break
                }

                val char2 = get(origin + bytesRead1 - 2).toInt() and 0xff
                val char3 = get(origin + bytesRead1 - 1).toInt() and 0xff
                if (char2 and 0xc0 != 0x80 || char3 and 0xc0 != 0x80) {
                    // Invalid UTF-8 - but we don't error out, we return what we *do* have
                    outBuffer[outIndex1++] = UTF_REPLACEMENT_CHAR
                }

                outBuffer[outIndex1++] =
                    ((c and 0x0f shl 12) or (char2 and 0x3f shl 6) or (char3 and 0x3f)).toChar()
            }

            else -> {
                // Invalid UTF-8 - but we don't error out, we return what we *do* have
                outBuffer[outIndex1++] = UTF_REPLACEMENT_CHAR
                break
            }
        }
    }
    return outIndex1
}

/**
 * Realign the ByteBuffer (forwards) to the next machine-word if required. If the `position` is
 * currently aligned to a machine-word this is a no-op.
 */
internal fun ByteBuffer.realign() {
    val p = position()
    val wordSize = if (NativeArch.is32bit) 4 else 8
    val aligned = (p or (wordSize - 1)) + 1

    position(aligned)
}
