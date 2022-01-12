package com.bugsnag.android.ndk.migrations

import android.content.Context
import androidx.test.platform.app.InstrumentationRegistry
import com.bugsnag.android.repackaged.dslplatform.json.DslJson
import org.junit.Before
import java.io.File

open class EventMigrationTest {

    private lateinit var context: Context
    private val json = DslJson<Map<String, Any>>()

    @Before
    fun setup() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
    }

    internal fun createTempFile(): File {
        return File.createTempFile("migrated_event", ".tmp", context.cacheDir).apply {
            deleteOnExit()
        }
    }

    internal fun parseJSON(file: File): Map<String, Any> {
        return deserialize(file.readBytes())
    }

    internal fun parseJSON(text: String): Map<String, Any> {
        return deserialize(text.toByteArray())
    }

    private fun deserialize(contents: ByteArray): Map<String, Any> {
        val result = json.deserialize(Map::class.java, contents, contents.size)
        @Suppress("UNCHECKED_CAST")
        return result as Map<String, Any>
    }

    companion object NativeLibs {
        init {
            System.loadLibrary("bugsnag-ndk")
            System.loadLibrary("bugsnag-ndk-test")
        }
    }
}
