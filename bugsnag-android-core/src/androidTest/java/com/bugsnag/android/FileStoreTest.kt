package com.bugsnag.android

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.io.FileNotFoundException
import java.lang.Exception
import java.lang.RuntimeException
import java.util.Comparator

class FileStoreTest {
    val appContext = ApplicationProvider.getApplicationContext<Context>()
    val config = Configuration("api-key")

    @Test
    fun sendsInternalErrorReport() {
        val delegate = CustomDelegate()
        val dir = File(appContext.filesDir, "custom-store")
        dir.mkdir()

        val store = CustomFileStore(appContext, dir.absolutePath, 1, null, delegate)
        val exc = RuntimeException("Whoops")
        store.write(CustomStreamable(exc))

        assertEquals("Crash report serialization", delegate.context)
        assertEquals(File(dir, "foo.json"), delegate.errorFile)
        assertEquals(exc, delegate.exception)
        assertEquals(0, dir.listFiles().size)
    }

    @Test
    fun sendsInternalErrorReportNdk() {
        val delegate = CustomDelegate()
        val dir = File(appContext.filesDir, "custom-store")
        dir.mkdir()

        val store = CustomFileStore(appContext, "", 1, null, delegate)
        store.enqueueContentForDelivery("foo")

        assertEquals("NDK Crash report copy", delegate.context)
        assertEquals(File("/foo.json"), delegate.errorFile)
        assertTrue(delegate.exception is FileNotFoundException)
    }
}

class CustomDelegate: FileStore.Delegate {
    var exception: Exception? = null
    var errorFile: File? = null
    var context: String? = null

    override fun onErrorIOFailure(exception: Exception?, errorFile: File?, context: String?) {
        this.exception = exception
        this.errorFile = errorFile
        this.context = context
    }
}

class CustomStreamable(val exc: Throwable) : JsonStream.Streamable {
    override fun toStream(stream: JsonStream) = throw exc
}

internal class CustomFileStore(
    appContext: Context,
    val folder: String?,
    maxStoreCount: Int,
    comparator: Comparator<File>?,
    delegate: Delegate?
) : FileStore<CustomStreamable>(appContext, folder, maxStoreCount, comparator, NoopLogger, delegate) {
    override fun getFilename(`object`: Any?) = "$folder/foo.json"
}
