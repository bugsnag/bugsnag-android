package com.bugsnag.android

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner

private const val MAX_STRING_LENGTH = 256
private const val BIG_STRING_LENGTH = MAX_STRING_LENGTH * 4

@RunWith(MockitoJUnitRunner::class)
class EventPayloadTrimTest {

    @Mock
    lateinit var client: Client

    @Test
    fun testTrimPayload() {
        val event = BugsnagTestUtils.generateEvent()
        event.addMetadata("trimming", "bigString", "*".repeat(BIG_STRING_LENGTH))

        // remove any existing threads to keep this test simple
        event.threads.clear()
        repeat(times = 10) { threadIdx ->
            event.addThread(threadIdx.toLong(), "test thread $threadIdx")
        }

        event.breadcrumbs.clear()
        repeat(times = 10) { breadcrumbIdx ->
            event.leaveBreadcrumb("breadcrumb $breadcrumbIdx")
        }

        val config = BugsnagTestUtils.generateImmutableConfig(
            Configuration("abc123").apply {
                maxBreadcrumbs = 2
                maxReportedThreads = 2
                maxStringValueLength = MAX_STRING_LENGTH
            }
        )

        val notifier = Notifier(name = "Test Notifier", version = "9.9.9")
        val payload = EventPayload(null, event, null, notifier, config)
        val trimmed = requireNotNull(payload.trimToSize(BIG_STRING_LENGTH).event)

        assertEquals(
            "${"*".repeat(MAX_STRING_LENGTH)}***<${BIG_STRING_LENGTH - MAX_STRING_LENGTH}> CHARS TRUNCATED***",
            trimmed.getMetadata("trimming")!!["bigString"]
        )

        val threads = trimmed.threads
        assertEquals(3, threads.size)
        assertEquals("test thread 0", threads[0].name)
        assertEquals("test thread 1", threads[1].name)
        assertEquals(
            "[8 threads omitted as the maxReportedThreads limit (2) was exceeded]",
            threads[2].name
        )

        val breadcrumbs = trimmed.breadcrumbs
        assertEquals(1, breadcrumbs.size)
        assertEquals("Removed, along with 9 older breadcrumbs, to reduce payload size", breadcrumbs[0].message)
    }
}
