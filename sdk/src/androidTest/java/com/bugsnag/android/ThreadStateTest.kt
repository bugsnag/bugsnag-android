package com.bugsnag.android

import android.support.test.filters.SmallTest
import android.support.test.runner.AndroidJUnit4
import com.bugsnag.android.BugsnagTestUtils.streamableToJsonArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
class ThreadStateTest {

    private val threadState = ThreadState(Configuration("api-key"), Thread.currentThread())
    private val json = streamableToJsonArray(threadState)

    /**
     * Verifies that the required values for 'thread' are serialised as an array
     */
    @Test
    fun testSerialisation() {
        for (k in 0 until json.length()) {
            val thread = json[k] as JSONObject
            assertNotNull(thread.getString("id"))
            assertNotNull(thread.getString("name"))
            assertNotNull(thread.getString("stacktrace"))
            assertEquals("android", thread.getString("type"))
        }
    }

    /**
     * Verifies that the current thread is serialised as an object, and that only this value
     * contains the errorReportingThread boolean flag
     */
    @Test
    fun testCurrentThread() {
        val currentThreadId = Thread.currentThread().id
        var currentThreadCount = 0

        for (k in 0 until json.length()) {
            val thread = json[k] as JSONObject
            val threadId = thread.getLong("id")

            if (threadId == currentThreadId) {
                assertTrue(thread.getBoolean("errorReportingThread"))
                currentThreadCount++
            } else {
                assertFalse(thread.has("errorReportingThread"))
            }
        }
        assertEquals(1, currentThreadCount)
    }

    /**
     * Verifies that a thread different from the current thread is serialised as an object,
     * and that only this value contains the errorReportingThread boolean flag
     */
    @Test
    fun testDifferentThread() {
        val otherThread = Thread.getAllStackTraces()
            .filter { it.key != Thread.currentThread() }
            .map { it.key }
            .first()

        val json = streamableToJsonArray(ThreadState(Configuration("api-key"), otherThread))
        var currentThreadCount = 0

        for (k in 0 until json.length()) {
            val thread = json[k] as JSONObject
            val threadId = thread.getLong("id")

            if (threadId == otherThread.id) {
                assertTrue(thread.getBoolean("errorReportingThread"))
                currentThreadCount++
            } else {
                assertFalse(thread.has("errorReportingThread"))
            }
        }
        assertEquals(1, currentThreadCount)
    }
}
