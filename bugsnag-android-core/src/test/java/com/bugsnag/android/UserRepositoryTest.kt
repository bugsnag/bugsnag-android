package com.bugsnag.android

import android.content.SharedPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.contains
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class UserRepositoryTest {

    @Mock
    lateinit var prefs: SharedPreferences

    @Mock
    lateinit var editor: SharedPreferences.Editor

    @Before
    fun setUp() {
        `when`(prefs.edit()).thenReturn(editor)
        `when`(editor.putString(anyString(), any())).thenReturn(editor)
        `when`(editor.remove(anyString())).thenReturn(editor)
    }

    @Test
    fun loadWithPersist() {
        `when`(prefs.getString(contains("user.id"), any())).thenReturn("jf123")
        `when`(prefs.getString(contains("user.name"), any())).thenReturn("Jane Fonda")
        `when`(prefs.getString(contains("user.email"), any())).thenReturn("test@example.com")

        val repository = UserRepository(prefs, true, "0asdf")
        val user = repository.load()

        assertEquals("jf123", user.id)
        assertEquals("Jane Fonda", user.name)
        assertEquals("test@example.com", user.email)
    }

    @Test
    fun loadNoPersist() {
        val repository = UserRepository(prefs, false, "device-id-123")
        val user = repository.load()
        assertEquals("device-id-123", user.id)
        assertNull(user.email)
        assertNull(user.name)
    }

    @Test
    fun saveWithPersist() {
        val repository = UserRepository(prefs, true, "")
        repository.save(User("123", "joe@yahoo.com", "Joe Bloggs"))
        verify(editor, times(1)).putString("user.id", "123")
        verify(editor, times(1)).putString("user.email", "joe@yahoo.com")
        verify(editor, times(1)).putString("user.name", "Joe Bloggs")
        verify(editor, times(1)).apply()
    }

    @Test
    fun saveNoPersist() {
        val repository = UserRepository(prefs, false, "")
        repository.save(User("123", "joe@yahoo.com", "Joe Bloggs"))
        verify(editor, times(1)).remove("user.id")
        verify(editor, times(1)).remove("user.email")
        verify(editor, times(1)).remove("user.name")
        verify(editor, times(1)).apply()
    }
}
