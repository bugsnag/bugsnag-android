package com.bugsnag.android

import android.os.RemoteException
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.util.concurrent.ExecutionException

class ThrowableCauseMatchingTest {
    @Test
    fun matchSingle() {
        val root = RuntimeException()
        assertTrue(root.anyCauseMatches { it is RuntimeException })
        assertFalse(root.anyCauseMatches { it is RemoteException })
    }

    @Test
    fun simpleChain() {
        val root = IOException()
        val wrapper1 = RuntimeException(root)
        val wrapper2 = ExecutionException(wrapper1)

        assertTrue(wrapper2.anyCauseMatches { it is IOException })
        assertTrue(wrapper2.anyCauseMatches { it is RuntimeException })
        assertTrue(wrapper2.anyCauseMatches { it is ExecutionException })
        assertFalse(wrapper2.anyCauseMatches { it is RemoteException })
    }

    @Test
    fun complexRecursiveChain() {
        val root = IOException("Root cause")
        val wrapper1 = RuntimeException("Wrapper 1", root)
        val wrapper2 = ExecutionException("Wrapper 2", wrapper1)
        val wrapper3 = IllegalStateException("Wrapper 3", wrapper2)
        val wrapper4 = IllegalArgumentException("Wrapper 4")
        wrapper4.initCause(wrapper3)

        // Create circular reference: root points back to wrapper4
        root.initCause(wrapper4)

        assertTrue(wrapper4.anyCauseMatches { it is IOException })
        assertTrue(wrapper4.anyCauseMatches { it is RuntimeException })
        assertTrue(wrapper4.anyCauseMatches { it is ExecutionException })
        assertTrue(wrapper4.anyCauseMatches { it is IllegalStateException })
        assertTrue(wrapper4.anyCauseMatches { it is IllegalArgumentException })
        assertFalse(wrapper4.anyCauseMatches { it is RemoteException })
    }
}
