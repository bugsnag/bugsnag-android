package com.bugsnag.android.ndk

import com.bugsnag.android.Client
import com.bugsnag.android.Logger
import com.bugsnag.android.NativeInterface
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import java.io.File

@RunWith(MockitoJUnitRunner::class)
class ReportDiscardScannerTest {
    @Mock
    lateinit var client: Client

    @Before
    fun setupNativeInterface() {
        NativeInterface.setClient(client)
    }

    @Test
    fun discardStaticData() {
        val discardScanner = ReportDiscardScanner(object : Logger {}, emptySet())
        assertTrue(discardScanner.shouldDiscard(File("/data/data/something/there_is_some.static_data.json")))
    }

    @Test
    fun discardNonJson() {
        val discardScanner = ReportDiscardScanner(object : Logger {}, emptySet())
        assertTrue(discardScanner.shouldDiscard(File("/data/data/683c6b92-b325-4987-80ad-77086509ca1e.dump")))
        assertTrue(discardScanner.shouldDiscard(File("/data/data/683c6b92-b325-4987-80ad-77086509ca1e.binary")))
        assertTrue(discardScanner.shouldDiscard(File("/data/data/something_not_quite.static_data.binary")))
        assertTrue(discardScanner.shouldDiscard(File("/data/data/data.binary")))
    }
}
