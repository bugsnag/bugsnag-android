package com.bugsnag.android

import android.content.Context
import android.content.SharedPreferences
import androidx.test.core.app.ApplicationProvider
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.io.File

internal class UserStoreTest {

    lateinit var file: File
    lateinit var storageDir: File
    lateinit var prefs: SharedPreferences
    lateinit var prefMigrator: SharedPrefMigrator
    lateinit var ctx: Context

    @Before
    fun setUp() {
        ctx = ApplicationProvider.getApplicationContext<Context>()
        storageDir = ctx.cacheDir
        file = File(storageDir, "user.json")
        file.delete()
        prefs = ctx.getSharedPreferences("com.bugsnag.android", Context.MODE_PRIVATE)
        prefMigrator = SharedPrefMigrator(ctx)
    }

    @After
    fun tearDown() {
        storageDir.delete()
        prefs.edit().clear().commit()
    }

    private fun generateConfig(persistUser: Boolean): ImmutableConfig {
        val config = BugsnagTestUtils.generateConfiguration()
        config.persistUser = persistUser
        return BugsnagTestUtils.convert(config)
    }

    @Test
    fun sharedPrefMigration() {
        prefs.edit()
            .putString("install.iud", "abc909fd")
            .putString("user.id", "jf123")
            .putString("user.email", "test@example.com")
            .putString("user.name", "Jane Fonda")
            .commit()

        val store = UserStore(generateConfig(true), "0asdf", file, prefMigrator, NoopLogger)
        val user = store.load(User()).user
        assertEquals("jf123", user.id)
        assertEquals("Jane Fonda", user.name)
        assertEquals("test@example.com", user.email)
    }

    /**
     * A file should be created if it does not already exist
     */
    @Test
    fun nonExistentFile() {
        val nonExistentFile = File(storageDir, "foo")
        nonExistentFile.delete()
        val store = UserStore(generateConfig(true), "device-id", file, prefMigrator, NoopLogger)
        val user = store.load(User()).user
        assertEquals("device-id", user.id)
        assertNull(user.email)
        assertNull(user.name)
    }

    /**
     * An empty file should return the default user
     */
    @Test
    fun emptyFile() {
        val store = UserStore(generateConfig(true), "device-id", file, prefMigrator, NoopLogger)
        val user = store.load(User()).user
        assertEquals("device-id", user.id)
        assertNull(user.email)
        assertNull(user.name)
    }

    /**
     * A file of the correct length with invalid contents should return the default user
     */
    @Test
    fun invalidFileContents() {
        file.writeText("{\"hamster\": 2}")
        val store = UserStore(generateConfig(true), "device-id", file, prefMigrator, NoopLogger)
        val user = store.load(User()).user
        assertEquals("device-id", user.id)
        assertNull(user.email)
        assertNull(user.name)
    }

    /**
     * A non-writable file does not crash the app
     */
    @Test
    fun nonWritableFile() {
        val nonReadableFile = File(storageDir, "foo").apply {
            delete()
            createNewFile()
            setWritable(false)
        }
        val store = UserStore(
            generateConfig(true),
            "device-id",
            nonReadableFile,
            prefMigrator,
            NoopLogger
        )
        val user = store.load(User()).user
        assertEquals("device-id", user.id)
        assertNull(user.email)
        assertNull(user.name)
    }

    /**
     * A valid file with persisted user information should be readable
     */
    @Test
    fun validFileContents() {
        file.writeText("{\"id\":\"jf123\",\"email\":\"test@example.com\",\"name\":\"Jane Fonda\"}")

        val store = UserStore(generateConfig(true), "0asdf", file, prefMigrator, NoopLogger)
        val user = store.load(User()).user
        assertEquals("jf123", user.id)
        assertEquals("Jane Fonda", user.name)
        assertEquals("test@example.com", user.email)
    }

    /**
     * If persistUser is false a user is still returned
     */
    @Test
    fun loadWithoutPersistUser() {
        val store = UserStore(
            generateConfig(false),
            "device-id-123",
            file,
            prefMigrator,
            NoopLogger
        )
        val user = store.load(User()).user
        assertEquals("device-id-123", user.id)
        assertNull(user.email)
        assertNull(user.name)
        assertEquals("", file.readText())
    }

    /**
     * If persistUser is false a user is not saved
     */
    @Test
    fun saveWithoutPersistUser() {
        val store = UserStore(generateConfig(false), "", file, prefMigrator, NoopLogger)
        store.save(User("123", "joe@yahoo.com", "Joe Bloggs"))
        assertEquals("", file.readText())
    }

    /**
     * Saving user when persistUser is true
     */
    @Test
    fun saveWithPersistUser() {
        val store = UserStore(generateConfig(true), "0asdf", file, prefMigrator, NoopLogger)
        val user = User("jf123", "test@example.com", "Jane Fonda")
        store.save(user)
        val expected = "{\"id\":\"jf123\",\"email\":\"test@example.com\",\"name\":\"Jane Fonda\"}"
        assertEquals(expected, file.readText())
    }

    /**
     * The user must have been changed in order for disk IO to occur when persisting.
     */
    @Test
    fun userRequiresChangeForDiskIO() {
        val store = UserStore(generateConfig(true), "0asdf", file, prefMigrator, NoopLogger)
        val user = User("jf123", "test@example.com", "Jane Fonda")
        store.save(user)

        // overwrite the previous save to test that IO doesn't happen without a change
        file.writeText("")

        // no change == no IO
        store.save(user)
        assertEquals("", file.readText())

        // change == IO
        store.save(User("abc", "joe@test.com", "Joe"))
        val expected = "{\"id\":\"abc\",\"email\":\"joe@test.com\",\"name\":\"Joe\"}"
        assertEquals(expected, file.readText())
    }
}
