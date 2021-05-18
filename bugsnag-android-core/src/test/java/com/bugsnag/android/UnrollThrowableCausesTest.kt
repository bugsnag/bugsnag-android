package com.bugsnag.android

import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test
import java.io.IOException
import java.lang.RuntimeException
import java.util.concurrent.ExecutionException

class UnrollThrowableCausesTest {
    @Test
    fun unrollSingle() {
        val root = RuntimeException()
        val chain = root.safeUnrollCauses()

        assertEquals(1, chain.size)
        assertSame(root, chain[0])
    }

    @Test
    fun unrollSimple() {
        val root = IOException()
        val wrapper1 = RuntimeException(root)
        val wrapper2 = ExecutionException(wrapper1)

        val chain = wrapper2.safeUnrollCauses()
        assertEquals(3, chain.size)
        assertSame(wrapper2, chain[0])
        assertSame(wrapper1, chain[1])
        assertSame(root, chain[2])
    }

    @Test
    fun unrollRecursive() {
        val fakeRoot = IOException()
        val wrapper1 = RuntimeException(fakeRoot)
        val wrapper2 = ExecutionException(wrapper1)
        val wrapper3 = ExecutionException(wrapper2)

        // make it recursive
        fakeRoot.initCause(wrapper2)

        val chain = wrapper3.safeUnrollCauses()
        assertEquals(4, chain.size)
        assertSame(wrapper3, chain[0])
        assertSame(wrapper2, chain[1])
        assertSame(wrapper1, chain[2])
        assertSame(fakeRoot, chain[3])
    }
}
