package com.bugsnag.android

import com.bugsnag.android.SessionStore.Companion.SESSION_COMPARATOR
import org.junit.Assert
import org.junit.Test
import java.io.File

class SessionStoreTest {
    @Test
    fun testComparator() {
        val first = "1504255147933d06e6168-1c10-4727-80d8-627a5111775b.json"
        val second = "1505000000000ef070b5b-fc0f-411e-8630-429acc477982.json"
        val startup = "150450000000053a27e4e-967c-4e5c-91be-2e86f2eb7cdc.json"

        // handle defaults
        Assert.assertEquals(0, SESSION_COMPARATOR.compare(null, null).toLong())
        Assert.assertEquals(
            -1,
            SESSION_COMPARATOR.compare(File(""), null).toLong()
        )
        Assert.assertEquals(1, SESSION_COMPARATOR.compare(null, File("")).toLong())

        // same value should always be 0
        Assert.assertEquals(
            0,
            SESSION_COMPARATOR.compare(File(first), File(first)).toLong()
        )
        Assert.assertEquals(
            0,
            SESSION_COMPARATOR.compare(File(startup), File(startup)).toLong()
        )

        // first is before second
        Assert.assertTrue(
            SESSION_COMPARATOR.compare(
                File(first),
                File(second)
            ) < 0
        )
        Assert.assertTrue(
            SESSION_COMPARATOR.compare(
                File(second),
                File(first)
            ) > 0
        )

        // startup is handled correctly
        Assert.assertTrue(
            SESSION_COMPARATOR.compare(
                File(first),
                File(startup)
            ) < 0
        )
        Assert.assertTrue(
            SESSION_COMPARATOR.compare(
                File(second),
                File(startup)
            ) > 0
        )
    }
}
