package com.bugsnag.android

import android.content.Context
import android.util.JsonReader
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.io.EOFException
import java.io.File
import java.io.FileNotFoundException
import java.lang.IllegalStateException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors

internal class SynchronizedStreamableStoreTest {

    private val user = User("123", "test@example.com", "Tess Tng")

    @Test
    fun testPersistNonExistingFile() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        val file = File(ctx.cacheDir, "no-such-file.json")
        val store = SynchronizedStreamableStore<User>(file)
        store.persist(user)
        assertEquals(user, store.load(User.Companion::fromReader))
    }

    @Test
    fun testPersistWritableFile() {
        val file = File.createTempFile("test", "json")
        val store = SynchronizedStreamableStore<User>(file)
        store.persist(user)
        assertEquals(user, store.load(User.Companion::fromReader))
    }

    @Test(expected = FileNotFoundException::class)
    fun testPersistNonWritableFile() {
        val file = File.createTempFile("test", "json").apply {
            setWritable(false)
        }
        val store = SynchronizedStreamableStore<User>(file)
        store.persist(user)
        assertNull(store.load(User.Companion::fromReader))
    }

    @Test(expected = IllegalStateException::class)
    fun testPersistExceptionInStreamable() {
        val file = File.createTempFile("test", "json")
        val store = SynchronizedStreamableStore<CrashyStreamable>(file)
        store.persist(CrashyStreamable())
        assertNull(store.load(CrashyStreamable.Companion::fromReader))
    }

    @Test(expected = FileNotFoundException::class)
    fun testReadNonExistingFile() {
        val file = File("no-such-file.bmp")
        val store = SynchronizedStreamableStore<User>(file)
        assertNull(store.load(User.Companion::fromReader))
    }

    @Test(expected = EOFException::class)
    fun testReadNonWritableFile() {
        val file = File.createTempFile("test", "json").apply {
            setWritable(false)
        }
        val store = SynchronizedStreamableStore<User>(file)
        assertNull(store.load(User.Companion::fromReader))
    }

    /**
     * Reads the same file concurrently to assert that a [ReadWriteLock] is used
     */
    @Test(timeout = 2000)
    fun testConcurrentReadsPossible() {
        // persist some initial data
        val file = File.createTempFile("test", "json")
        val store = SynchronizedStreamableStore<ThreadTestStreamable>(file)
        store.persist(ThreadTestStreamable("some_val"))

        // read file on bg thread, triggered halfway through reading file on main thread
        var alreadyReadingBgThread = false
        ThreadTestStreamable.readCallback = {
            if (!alreadyReadingBgThread) {
                alreadyReadingBgThread = true
                val reader = JsonReader(file.reader())
                val latch = CountDownLatch(1)

                Executors.newSingleThreadExecutor().execute {
                    val bgThreadObj = ThreadTestStreamable.fromReader(reader)
                    assertEquals("some_val", bgThreadObj.id)
                    latch.countDown()
                }
                latch.await()
            }
        }

        // read the file on the main thread
        val reader = JsonReader(file.reader())
        val mainThreadObj = ThreadTestStreamable.fromReader(reader)
        assertEquals("some_val", mainThreadObj.id)
    }
}

internal class ThreadTestStreamable(
    val id: String,
    val writeCallback: () -> Unit = {}
) : JsonStream.Streamable {

    override fun toStream(stream: JsonStream) {
        with(stream) {
            beginObject()
            name("test")
            writeCallback()
            value(id)
            endObject()
        }
    }

    companion object : JsonReadable<ThreadTestStreamable> {
        var readCallback: () -> Unit = {}

        override fun fromReader(reader: JsonReader): ThreadTestStreamable {
            with(reader) {
                beginObject()
                nextName()
                readCallback()
                val obj = ThreadTestStreamable(nextString())
                endObject()
                return obj
            }
        }
    }
}

internal class CrashyStreamable : JsonStream.Streamable {
    override fun toStream(stream: JsonStream) = throw IllegalStateException()

    companion object : JsonReadable<CrashyStreamable> {
        override fun fromReader(reader: JsonReader) = throw IllegalStateException()
    }
}
