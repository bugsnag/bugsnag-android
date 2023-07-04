package com.bugsnag.android

import android.content.Context
import android.content.SharedPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mock
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
internal class SharedPrefMigratorTest {

    @Mock
    lateinit var context: Context

    @Mock
    lateinit var prefs: SharedPreferences

    @Mock
    lateinit var editor: SharedPreferences.Editor

    lateinit var prefMigrator: SharedPrefMigrator

    @Before
    fun setUp() {
        `when`(context.getSharedPreferences(eq("com.bugsnag.android"), anyInt())).thenReturn(prefs)
        prefMigrator = SharedPrefMigrator(context)
    }

    @Test
    fun nullSharedPreferences() {
        `when`(context.getSharedPreferences(eq("com.bugsnag.android"), anyInt())).thenReturn(null)
        prefMigrator = SharedPrefMigrator(context)
        assertFalse(prefMigrator.hasPrefs())
    }

    @Test
    fun gettingSharedPreferencesWithException() {
        `when`(context.getSharedPreferences(eq("com.bugsnag.android"), anyInt())).thenThrow(RuntimeException())
        prefMigrator = SharedPrefMigrator(context)
        assertFalse(prefMigrator.hasPrefs())
    }

    @Test
    fun nullDeviceId() {
        `when`(prefs.getString("install.iud", null)).thenReturn(null)
        assertNull(prefMigrator.loadDeviceId(true))
    }

    @Test
    fun validDeviceId() {
        `when`(prefs.getString("install.iud", null)).thenReturn("f09asdfb")
        assertEquals("f09asdfb", prefMigrator.loadDeviceId(true))
    }

    @Test
    fun emptyUser() {
        `when`(prefs.getString("user.id", "f09asdfb")).thenReturn("f09asdfb")
        `when`(prefs.getString("user.email", null)).thenReturn(null)
        `when`(prefs.getString("user.name", null)).thenReturn(null)

        val observed = prefMigrator.loadUser("f09asdfb")
        assertEquals("f09asdfb", observed.id)
        assertNull(observed.email)
        assertNull(observed.name)
    }

    @Test
    fun populatedUser() {
        `when`(prefs.getString("user.id", "f09asdfb")).thenReturn("abc75092")
        `when`(prefs.getString("user.email", null)).thenReturn("test@example.com")
        `when`(prefs.getString("user.name", null)).thenReturn("Joe")

        val expected = User("abc75092", "test@example.com", "Joe")
        val observed = prefMigrator.loadUser("f09asdfb")
        assertEquals(expected.id, observed.id)
        assertEquals(expected.email, observed.email)
        assertEquals(expected.name, observed.name)
    }

    @Test
    fun deletePrefs() {
        `when`(prefs.contains("install.iud")).thenReturn(true)
        `when`(prefs.edit()).thenReturn(editor)
        `when`(editor.clear()).thenReturn(editor)
        prefMigrator.deleteLegacyPrefs()
        verify(editor, times(1)).clear()
        verify(editor, times(1)).commit()
    }

    @Test
    fun hasPrefsTrue() {
        `when`(prefs.contains("install.iud")).thenReturn(true)
        assertTrue(prefMigrator.hasPrefs())
    }

    @Test
    fun hasPrefsFalse() {
        `when`(prefs.contains("install.iud")).thenReturn(false)
        assertFalse(prefMigrator.hasPrefs())
    }
}
