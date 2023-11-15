package com.bugsnag.android

import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.Mockito.mock

class TraceParserTest {

    private val mockLogger = mock(Logger::class.java)

    private val expectedThreadNames = listOf(
        "Thread-3",
        "Signal Catcher",
        "main",
        "perfetto_hprof_listener",
        "ADB-JDWP Connection Control Thread",
        "ReferenceQueueDaemon",
        "FinalizerDaemon",
        "Jit thread pool worker thread 0",
        "HeapTaskDaemon",
        "FinalizerWatchdogDaemon",
        "binder:11442_1",
        "binder:11442_2",
        "binder:11442_3",
        "binder:11442_4",
        "Profile Saver",
        "Bad \"name\"",
        "Bugsnag IO thread",
        "bugsnag-anr-collector",
        "Bugsnag Error thread",
        "Bugsnag Session thread",
        "ConnectivityThread",
        "RenderThread",
        "hwuiTask0",
        "hwuiTask1",
        "binder:11442_5",
        "binder:11442_1",
    )

    private val expectedFirstThreadStacktrace: List<Stackframe> = listOf(
        Stackframe("java.util.AbstractCollection.<init>", "AbstractCollection.java", 66, null),
        Stackframe("java.util.AbstractList.<init>", "AbstractList.java", 78, null),
        Stackframe("java.util.ArrayList.<init>", "ArrayList.java", 178, null),
        Stackframe("com.bugsnag.android.EventInternal.<init>", "EventInternal.kt", 22, true),
        Stackframe("com.bugsnag.android.Event.<init>", "Event.java", 38, true),
        Stackframe(
            "com.bugsnag.android.NativeInterface.createEvent",
            "NativeInterface.java",
            550,
            true
        ),
        Stackframe("com.bugsnag.android.AnrPlugin.notifyAnrDetected", "AnrPlugin.kt", 111, true),
    )

    private val expectedLastThreadStacktrace: List<Stackframe> = listOf(
        Stackframe(
            "syscall",
            "/apex/com.android.runtime/lib64/bionic/libc.so",
            0x4df5cL,
            null
        ).apply { codeIdentifier = "01331f74b0bb2cb958bdc15282b8ec7b" },
        Stackframe(
            "__futex_wait_ex(void volatile*, bool, int, bool, timespec const*)",
            "/apex/com.android.runtime/lib64/bionic/libc.so",
            0x52664L,
            null
        ).apply { codeIdentifier = "01331f74b0bb2cb958bdc15282b8ec7b" },
        Stackframe(
            "pthread_cond_wait",
            "/apex/com.android.runtime/lib64/bionic/libc.so",
            0xb56ccL,
            null
        ).apply { codeIdentifier = "01331f74b0bb2cb958bdc15282b8ec7b" },
        Stackframe(
            "std::__1::condition_variable::wait(std::__1::unique_lock<std::__1::mutex>&)",
            "/system/lib64/libc++.so",
            0x699e0L,
            null
        ).apply { codeIdentifier = "6ae0290e5bfb8abb216bde2a4ee48d9e" },
        Stackframe(
            "android::AsyncWorker::run()",
            "/system/lib64/libgui.so",
            0xa048cL,
            null
        ).apply { codeIdentifier = "383a37b5342fd0249afb25e7134deb33" },
        Stackframe(
            "void* std::__1::__thread_proxy<std::__1::tuple<std::__1::unique_ptr<std::__1::__thread_struct, std::__1::default_delete<std::__1::__thread_struct> >, void (android::AsyncWorker::*)(), android::AsyncWorker*> >(void*)",
            "/system/lib64/libgui.so",
            0xa0878L,
            null
        ).apply { codeIdentifier = "383a37b5342fd0249afb25e7134deb33" },
        Stackframe(
            "__pthread_start(void*)",
            "/apex/com.android.runtime/lib64/bionic/libc.so",
            0xb63b0L,
            null
        ).apply { codeIdentifier = "01331f74b0bb2cb958bdc15282b8ec7b" },
        Stackframe(
            "__start_thread",
            "/apex/com.android.runtime/lib64/bionic/libc.so",
            0x530b8L,
            null
        ).apply { codeIdentifier = "01331f74b0bb2cb958bdc15282b8ec7b" },
    )

    @Test
    fun testParseAnr() {
        val traceParser = TraceParser(mockLogger, setOf("com.bugsnag"))
        val threads = mutableListOf<Thread>()
        traceParser.parse(this::class.java.getResourceAsStream("/emulator-exit-anr-trace")!!) {
            threads.add(it)
        }

        assertEquals(26, threads.size)
        assertEquals(expectedThreadNames, threads.map { it.name })

        val firstThread = threads.first()
        assertEquals("Thread-3", firstThread.name)
        assertEquals("3", firstThread.id)
        assertEquals(Thread.State.RUNNABLE, firstThread.state)
        val firstStacktrace = firstThread.stacktrace
        expectedFirstThreadStacktrace.forEachIndexed { index, stackframe ->
            assertStackframeEquals(stackframe, firstStacktrace[index])
        }

        val lastThread = threads.last()
        assertEquals("binder:11442_1", lastThread.name)
        assertEquals("11493", lastThread.id)
        assertEquals(Thread.State.WAITING, lastThread.state)
        val lastStacktrace = lastThread.stacktrace
        expectedLastThreadStacktrace.forEachIndexed { index, stackframe ->
            assertStackframeEquals(stackframe, lastStacktrace[index])
        }
    }

    private fun assertStackframeEquals(expected: Stackframe, actual: Stackframe) {
        assertEquals(expected.method, actual.method)
        assertEquals(expected.file, actual.file)
        assertEquals(expected.lineNumber, actual.lineNumber)
        assertEquals(expected.inProject, actual.inProject)
    }
}
