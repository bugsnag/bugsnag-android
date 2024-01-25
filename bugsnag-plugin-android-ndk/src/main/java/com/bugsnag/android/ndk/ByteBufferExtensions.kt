package com.bugsnag.android.ndk

import java.nio.ByteBuffer

internal fun ByteBuffer.getNativeInt(): Int = getInt()
internal fun ByteBuffer.getNativeLong(): Long = getLong()

internal fun ByteBuffer.getCString(byteCount: Int): String {
    position(position() + byteCount)
    return ""
}
