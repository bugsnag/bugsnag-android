package com.bugsnag.android

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.io.File

class ClientUserTest {

    private lateinit var context: Context
    private lateinit var config: Configuration
    private lateinit var client: Client

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        config = BugsnagTestUtils.generateConfiguration()
        context.cacheDir.deleteRecursively()
    }

    @After
    fun tearDown() {
        getSharedPrefs().edit()
            .remove("user.id")
            .remove("user.email")
            .remove("user.name")
            .commit()
        client.close()
    }

    private fun getSharedPrefs() = context.getSharedPreferences(
        "com.bugsnag.android",
        Context.MODE_PRIVATE
    )

    @Test
    fun testMigrateUserFromPrefs() {
        // Set a user in prefs
        getSharedPrefs().edit()
            .putString("install.iud", "f0a0bc9394")
            .putString("user.id", USER_ID)
            .putString("user.email", USER_EMAIL)
            .putString("user.name", USER_NAME)
            .commit()
        config.persistUser = true
        config.delivery = BugsnagTestUtils.generateDelivery()
        client = Client(context, config)

        lateinit var user: User
        client.addOnError(
            OnErrorCallback { event -> // Pull out the user information
                user = event.getUser()
                true
            }
        )
        client.notify(RuntimeException("Testing"))

        // Check the user details have been set
        assertEquals(USER_ID, user.id)
        assertEquals(USER_EMAIL, user.email)
        assertEquals(USER_NAME, user.name)

        // check preferences have been deleted
        assertFalse(getSharedPrefs().contains("install.iud"))
    }

    @Test
    fun testPersistUserEnabled() {
        config.persistUser = true
        client = Client(context, config)
        client.setUser(USER_ID, USER_EMAIL, USER_NAME)

        // Check that the user was persisted
        val file = File(client.config.persistenceDirectory.value, "bugsnag/user-info")
        val expected = "{\"id\":\"123456\",\"email\":\"mr.test@email.com\",\"name\":\"Mr Test\"}"
        assertEquals(expected, file.readText())
    }

    @Test
    fun testPersistUserDisabled() {
        config.persistUser = false
        client = Client(context, config)

        // Check that the user was not persisted
        val file = File(client.config.persistenceDirectory.value, "user-info")
        assertFalse(file.exists())
        client.setUser(USER_ID, USER_EMAIL, USER_NAME)
        assertFalse(file.exists())
    }

    @Test
    fun testUserCloned() {
        config.setUser("123", "test@example.com", "Tess Derby")
        client = Client(context, config)
        val user = client.getUser()
        assertEquals("123", user.id)
        assertEquals("Tess Derby", user.name)
        assertEquals("test@example.com", user.email)
    }

    @Test
    fun testUserNotCloned() {
        client = Client(context, config)
        val user = client.getUser()
        assertNotNull(user.id) // use an auto-generated-id
        assertNull(user.name)
        assertNull(user.email)
    }

    @Test
    fun testDeviceIdNotUserId() {
        config.setUser("123", "test@example.com", "Tess Derby")
        client = Client(context, config)
        assertEquals("123", client.getUser().id)
        val device = client.getDeviceDataCollector().generateDevice()
        assertNotEquals("123", device.id)
    }

    @Test
    fun testDeviceIdEqualsUserId() {
        client = Client(context, config)
        val userId = client.getUser().id
        val device = client.getDeviceDataCollector().generateDevice()
        assertEquals(userId, device.id)
    }

    companion object {
        private const val USER_ID = "123456"
        private const val USER_EMAIL = "mr.test@email.com"
        private const val USER_NAME = "Mr Test"
    }
}
