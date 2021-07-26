package com.bugsnag.android

import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
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
    }
}
