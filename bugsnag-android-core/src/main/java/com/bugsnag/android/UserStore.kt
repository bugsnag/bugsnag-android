package com.bugsnag.android

import android.util.Log
import com.bugsnag.android.internal.ImmutableConfig
import com.bugsnag.android.internal.StateObserver
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.util.concurrent.atomic.AtomicReference

/**
 * This class is responsible for persisting and retrieving user information.
 */
internal class UserStore @JvmOverloads constructor(
    private val config: ImmutableConfig,
    private val deviceId: String?,
    file: File = File(config.persistenceDirectory.value, "user-info"),
    private val sharedPrefMigrator: SharedPrefMigrator,
    private val logger: Logger
) {

    private val synchronizedStreamableStore: SynchronizedStreamableStore<User>
    private val previousLaunchFile: SynchronizedStreamableStore<User>
    private val persist = config.persistUser
    private val previousUser = AtomicReference<User?>(null)

    init {
        // check whether or not a user-info file exists from the previous launch
        this.previousLaunchFile = SynchronizedStreamableStore(file)

        // only create a user-info file if config.persistUser = true, so that we don't just have an empty file when config.persistUser = false
        if (persist) {
            try {
                file.createNewFile()
            } catch (exc: IOException) {
                logger.w("Failed to create device ID file", exc)
            }
        }
        this.synchronizedStreamableStore = SynchronizedStreamableStore(file)
    }

    /**
     * Loads the user state which should be used by the [Client]. This is supplied either from
     * the [Configuration] value, or a file in the [Configuration.getPersistenceDirectory] if
     * [Configuration.getPersistUser] is true.
     *
     * If no user is stored on disk, then a default [User] is used which uses the device ID
     * as its ID.
     *
     * The [UserState] provides a mechanism for observing value changes to its user property,
     * so to avoid interfering with this the method should only be called once for each [Client].
     */
    fun load(initialUser: User): UserState {
        val validConfigUser = validUser(initialUser)

        val loadedUser = when {
            validConfigUser -> initialUser
            persist -> loadPersistedUser()
            else -> null
        }

        val userState = when {
            loadedUser != null && validUser(loadedUser) -> UserState(loadedUser)
            else -> UserState(User(deviceId, null, null))
        }

        userState.addObserver(
            StateObserver { event ->
                if (event is StateEvent.UpdateUser) {
                    save(event.user)
                }
            }
        )
        return userState
    }

    /**
     * Persists the user if [Configuration.getPersistUser] is true and the object is different
     * from the previously persisted value.
     */
    fun save(user: User) {
        if (persist && user != previousUser.getAndSet(user)) {
            try {
                synchronizedStreamableStore.persist(user)
            } catch (exc: Exception) {
                logger.w("Failed to persist user info", exc)
            }
        }
    }

    private fun validUser(user: User) =
        user.id != null || user.name != null || user.email != null

    private fun loadPersistedUser(): User? {
        try {
            previousLaunchFile.load(User.Companion::fromReader)
        } catch (exc: Exception) {
            logger.w("No persisted user info has been found from the previous launch", exc)
        }

        return if (sharedPrefMigrator.hasPrefs()) {
            val legacyUser = sharedPrefMigrator.loadUser(deviceId)
            save(legacyUser)
            legacyUser
        } else {
            return try {
                synchronizedStreamableStore.load(User.Companion::fromReader)
            } catch (exc: Exception) {
                // when config.persistUser is first set to true, the file will have been created but not yet filled
                logger.w("No persisted user info has been loaded from the previous launch", exc)
                null
            }
        }
    }
}