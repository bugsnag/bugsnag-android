package com.bugsnag.android

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.bugsnag.android.EventStore.Companion.EVENT_COMPARATOR
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import java.io.File
import java.util.Comparator

@RunWith(MockitoJUnitRunner::class)
class FileStoreTest {

    @Test
    fun sendsInternalErrorReport() {

        val delegate = CustomDelegate()
        val dir = File(ApplicationProvider.getApplicationContext<Application>().cacheDir, "tmp")
        val store = CustomFileStore(dir, 1, EVENT_COMPARATOR, delegate)
        val exc = RuntimeException("Whoops")
        store.write(CustomStreamable(exc))

        assertEquals("Crash report serialization", delegate.context)
        assertEquals(File(dir, "foo.json"), delegate.errorFile)
        assertEquals(exc, delegate.exception)
        val files = requireNotNull(dir.listFiles())
        assertEquals(0, files.size)
    }
}

class CustomDelegate : FileStore.Delegate {
    var exception: Exception? = null
    var errorFile: File? = null
    var context: String? = null

    override fun onErrorIOFailure(exception: Exception?, errorFile: File?, context: String?) {
        this.exception = exception
        this.errorFile = errorFile
        this.context = context
    }
}

class CustomStreamable(private val exc: Throwable) : JsonStream.Streamable {
    override fun toStream(stream: JsonStream) = throw exc
}

internal class CustomFileStore(
    folder: File,
    maxStoreCount: Int,
    comparator: Comparator<in File?>,
    delegate: Delegate?
) : FileStore(folder, maxStoreCount, comparator, NoopLogger, delegate) {
    override fun getFilename(obj: Any?) = "foo.json"
}
