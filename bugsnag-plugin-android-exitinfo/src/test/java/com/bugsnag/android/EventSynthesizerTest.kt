package com.bugsnag.android

import android.app.ApplicationExitInfo
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class EventSynthesizerTest {
    private lateinit var eventSynthesizer: EventSynthesizer

    @Mock
    private lateinit var anrEventEnhancer: (Event, ApplicationExitInfo) -> Unit

    @Mock
    private lateinit var exitInfoPluginStore: ExitInfoPluginStore


    @Test
    fun createEventWithAnrExitInfo() {
    }
}
