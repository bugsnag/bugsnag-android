package com.bugsnag.android

import android.content.Context
import android.content.SharedPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
internal class SharedPrefMigratorTest {

    @Mock
    lateinit var context: Context

    @Mock
    lateinit var prefs: SharedPreferences

    lateinit var prefMigrator: SharedPrefMigrator

    @Before
    fun setUp() {
        `when`(context.getSharedPreferences(eq("com.bugsnag.android"), eq(0))).thenReturn(prefs)
        prefMigrator = SharedPrefMigrator(context)
    }

    @Test
    fun nullDeviceId() {
        `when`(prefs.getString("install.iud", null)).thenReturn(null)
        assertNull(prefMigrator.loadDeviceId())
    }

    @Test
    fun validDeviceId() {
        `when`(prefs.getString("install.iud", null)).thenReturn("f09asdfb")
        assertEquals("f09asdfb", prefMigrator.loadDeviceId())
    }
}
