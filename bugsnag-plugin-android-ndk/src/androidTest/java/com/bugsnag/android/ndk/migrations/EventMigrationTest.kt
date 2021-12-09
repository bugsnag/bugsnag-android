package com.bugsnag.android.ndk.migrations

import android.content.Context
import androidx.test.platform.app.InstrumentationRegistry
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.Before
import java.io.File

open class EventMigrationTest {

    private lateinit var context: Context
    private val objectMapper = ObjectMapper()

    @Before
    fun setup() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
    }

    internal fun createTempFile(): File {
        return File.createTempFile("migrated_event", ".tmp", context.cacheDir).apply {
            deleteOnExit()
        }
    }

    internal fun parseJSON(file: File): Map<*, *> {
        return objectMapper.readValue(file, Map::class.java)
    }

    internal fun parseJSON(text: String): Map<*, *> {
        return objectMapper.readValue(text, Map::class.java)
    }

    companion object NativeLibs {
        init {
            System.loadLibrary("bugsnag-ndk")
            System.loadLibrary("bugsnag-ndk-test")
        }
    }
}
