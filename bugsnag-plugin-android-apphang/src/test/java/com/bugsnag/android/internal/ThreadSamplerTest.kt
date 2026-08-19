package com.bugsnag.android.internal

import com.bugsnag.android.Event
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.Mockito.anyLong
import org.mockito.Mockito.anyString
import org.mockito.Mockito.atLeast
import org.mockito.Mockito.eq
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

class ThreadSamplerTest {

    @Test
    fun testSampleCountIncrementsWithEachSample() {
        val thread = Thread {
            Thread.sleep(1000)
        }
        thread.start()

        val sampler = ThreadSampler(thread)
        assertEquals(0, sampler.totalSamplesTaken)

        sampler.captureSample()
        assertEquals(1, sampler.totalSamplesTaken)

        sampler.captureSample()
        assertEquals(2, sampler.totalSamplesTaken)

        sampler.captureSample()
        assertEquals(3, sampler.totalSamplesTaken)

        thread.interrupt()
        thread.join()
    }

    @Test
    fun testResetSamplingClearsCount() {
        val startedLatch = CountDownLatch(1)
        val thread = Thread {
            startedLatch.countDown()
            Thread.sleep(1000)
        }
        thread.start()
        startedLatch.await(5, TimeUnit.SECONDS)

        val sampler = ThreadSampler(thread)
        sampler.captureSample()
        sampler.captureSample()
        assertEquals(2, sampler.totalSamplesTaken)

        sampler.resetSampling()
        assertEquals(0, sampler.totalSamplesTaken)

        thread.interrupt()
        thread.join()
    }

    @Test
    fun testCaptureSampleWithEmptyStack() {
        // Create a thread that finishes immediately
        val thread = Thread { }
        thread.start()
        thread.join()

        val sampler = ThreadSampler(thread)
        sampler.captureSample()

        // Should not increment when stack is empty
        assertEquals(0, sampler.totalSamplesTaken)
    }

    @Test
    fun testCreateErrorWithSingleSample() {
        val recursiveDepth = 5
        val startedLatch = CountDownLatch(1)
        val running = AtomicBoolean(true)

        val thread = Thread {
            startedLatch.countDown()
            while (running.get()) {
                recursiveMethod1(recursiveDepth)
            }
        }
        thread.start()
        startedLatch.await(5, TimeUnit.SECONDS)

        val sampler = ThreadSampler(thread)
        sampler.captureSample()

        val event = mock(Event::class.java)
        val error = mock(com.bugsnag.android.Error::class.java)
        `when`(event.addError(anyString(), anyString())).thenReturn(error)

        sampler.createError(event)

        verify(event).addError("AppHang", "Most frequent stack path (1 samples taken)")

        verify(error).addStackframe(eq("java.lang.Thread.run"), eq("Thread.java"), anyLong())
        verify(error, atLeast(recursiveDepth))
            .addStackframe(eq("${this::class.java.name}.recursiveMethod1"), anyString(), anyLong())

        running.set(false)
        thread.join()
    }

    @Test
    fun testCreateErrorWithMultipleSamples() {
        val recursiveDepth = 10
        val startedLatch = CountDownLatch(1)
        val running = AtomicBoolean(true)

        val thread = Thread {
            startedLatch.countDown()
            while (running.get()) {
                recursiveMethod1(recursiveDepth)
            }
        }
        thread.start()
        startedLatch.await(5, TimeUnit.SECONDS)

        val sampler = ThreadSampler(thread)

        // Take multiple samples
        repeat(5) {
            sampler.captureSample()
            Thread.sleep(10)
        }

        val event = mock(Event::class.java)
        val error = mock(com.bugsnag.android.Error::class.java)
        `when`(event.addError(anyString(), anyString())).thenReturn(error)

        sampler.createError(event)

        verify(event).addError("AppHang", "Most frequent stack path (5 samples taken)")

        verify(error).addStackframe(eq("java.lang.Thread.run"), eq("Thread.java"), anyLong())
        verify(error, atLeast(recursiveDepth))
            .addStackframe(eq("${this::class.java.name}.recursiveMethod1"), anyString(), anyLong())
        running.set(false)
        thread.join()
    }

    @Test
    fun testCreateErrorWithDifferentStackPaths() {
        val latch = CountDownLatch(1)
        val running = AtomicBoolean(true)
        val useMethod1 = AtomicBoolean(true)

        val thread = Thread {
            latch.countDown()
            while (running.get()) {
                if (useMethod1.get()) {
                    recursiveMethod1(5)
                } else {
                    recursiveMethod2(5)
                }
            }
        }
        thread.start()
        latch.await(5, TimeUnit.SECONDS)

        val sampler = ThreadSampler(thread)

        // Sample method1 more frequently
        repeat(3) {
            sampler.captureSample()
            Thread.sleep(10)
        }

        useMethod1.set(false)
        Thread.sleep(50)

        // Sample method2 less frequently
        sampler.captureSample()

        assertEquals(4, sampler.totalSamplesTaken)

        val event = mock(Event::class.java)
        val error = mock(com.bugsnag.android.Error::class.java)
        `when`(event.addError(anyString(), anyString())).thenReturn(error)

        sampler.createError(event)

        verify(event).addError("AppHang", "Most frequent stack path (4 samples taken)")

        running.set(false)
        thread.join()
    }

    // Helper methods to create distinct stack traces
    private fun recursiveMethod1(depth: Int) {
        if (depth > 0) {
            recursiveMethod1(depth - 1)
        } else {
            Thread.sleep(10)
        }
    }

    private fun recursiveMethod2(depth: Int) {
        if (depth > 0) {
            recursiveMethod2(depth - 1)
        } else {
            Thread.sleep(10)
        }
    }
}
