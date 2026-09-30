package com.bugsnag.android

import com.bugsnag.android.internal.BackgroundTaskService
import com.bugsnag.android.internal.TaskType
import com.bugsnag.android.internal.dag.Provider
import java.io.File
import java.util.concurrent.Future
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.atomic.AtomicReference

/**
 * This class is responsible for persisting and retrieving user information.
 */
internal data class UserStoreServices(
    val logger: Logger,
    val bgTaskService: BackgroundTaskService = BackgroundTaskService()
)

internal class UserStore(
    private val persist: Boolean,
    private val persistentDir: Provider<File>,
    private val deviceIdStore: Provider<DeviceIdStore.DeviceIds?>,
    file: File = File(persistentDir.get(), "user-info"),
    private val sharedPrefMigrator: Provider<SharedPrefMigrator>,
    private val services: UserStoreServices
) {

    private val synchronizedStreamableStore: SynchronizedStreamableStore<User>
    private val previousUser = AtomicReference<User?>(null)

    init {
        this.synchronizedStreamableStore = SynchronizedStreamableStore(file)
    }

    /**
     * Loads the user state which should be used by the [Client]. This is supplied either from
     * the [Configuration] value, or a file in the [Configuration.getPersistenceDirectory] if
     * [Configuration.getPersistUser] is true.
     *
     * If no user is stored on disk, then a default [User] is used which uses the device ID
     * as its ID (unless the generateAnonymousId config option is set to false, in which case the
     * device ID and therefore the user ID is set to
     * null).
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
            // if generateAnonymousId config option is false, the deviceId should already be null
            // here
            else -> UserState(User(deviceIdStore.get()?.deviceId, null, null))
        }

        userState.addObserver { event ->
            if (event is StateEvent.UpdateUser) {
                save(event.user)
            }
        }
        return userState
    }

    /**
     * Schedules persistence if [Configuration.getPersistUser] is true and the object is different
     * from the previously persisted value. This deliberately does not wait for disk I/O so callers
     * such as `setUser` are never blocked.
     */
    fun save(user: User): Future<*>? {
        if (persist && user != previousUser.getAndSet(user)) {
            try {
                return services.bgTaskService.submitTask(TaskType.IO) {
                    persistUser(user)
                }
            } catch (exc: RejectedExecutionException) {
                services.logger.w("Failed to schedule user persistence", exc)
            }
        }
        return null
    }

    private fun persistUser(user: User) {
        try {
            synchronizedStreamableStore.persist(user)
        } catch (exc: Exception) {
            services.logger.w("Failed to persist user info", exc)
        }
    }

    private fun validUser(user: User) =
        user.id != null || user.name != null || user.email != null

    private fun loadPersistedUser(): User? {
        return if (sharedPrefMigrator.get().hasPrefs()) {
            val legacyUser = sharedPrefMigrator.get().loadUser(deviceIdStore.get()?.deviceId)
            // Persist the migrated user before legacy preferences are asynchronously removed.
            if (persist) {
                persistUser(legacyUser)
            }
            legacyUser
        } else if (
            synchronizedStreamableStore.file.canRead() &&
            synchronizedStreamableStore.file.length() > 0L &&
            persist
        ) {
            try {
                synchronizedStreamableStore.load(User.Companion::fromReader)
            } catch (exc: Exception) {
                services.logger.w("Failed to load user info", exc)
                null
            }
        } else {
            null
        }
    }
}
