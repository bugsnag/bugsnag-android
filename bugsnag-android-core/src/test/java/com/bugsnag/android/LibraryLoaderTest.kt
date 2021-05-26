package com.bugsnag.android

import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.any
import org.mockito.Mock
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class LibraryLoaderTest {

    @Mock
    lateinit var client: Client

    @Test
    fun loadMissingLibrary() {
        val libraryLoader = LibraryLoader()
        val loaded = libraryLoader.loadLibrary("foo", client) { true }
        assertFalse(loaded)
        assertFalse(libraryLoader.isLoaded)
        verify(client, times(1)).notify(any(), any())
    }

    @Test
    fun loadCalledOnce() {
        val libraryLoader = LibraryLoader()
        var loaded = libraryLoader.loadLibrary("foo", client) { true }
        assertFalse(loaded)
        assertFalse(libraryLoader.isLoaded)

        // duplicate calls only invoke System.loadLibrary once
        loaded = libraryLoader.loadLibrary("foo", client) { true }
        assertFalse(loaded)
        assertFalse(libraryLoader.isLoaded)
        verify(client, times(1)).notify(any(), any())
    }
}
