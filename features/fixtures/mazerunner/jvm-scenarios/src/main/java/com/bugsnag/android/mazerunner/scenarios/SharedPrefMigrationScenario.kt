package com.bugsnag.android.mazerunner.scenarios

import android.annotation.SuppressLint
import android.content.Context
import com.bugsnag.android.Bugsnag
import com.bugsnag.android.Configuration
import com.bugsnag.android.mazerunner.BugsnagIntentParams
import java.io.File

/**
 * User/device information is migrated from the legacy [SharedPreferences] location
 */
internal class SharedPrefMigrationScenario(
    config: Configuration,
    context: Context,
    eventMetadata: String
) : Scenario(config, context, eventMetadata) {

    init {
        config.persistUser = true
    }

    override fun startBugsnag(startBugsnagOnly: Boolean) {
        persistLegacyPrefs()
        // make sure there is no "leftover" device-id file to interfere with the test
        File(context.filesDir, "device-id").delete()
        super.startBugsnag(startBugsnagOnly)
    }

    override fun startScenario() {
        super.startScenario()
        if (!isRunningFromBackgroundService()) {
            val scenarioParams = BugsnagIntentParams(
                javaClass.simpleName,
                config.apiKey,
                config.endpoints.notify,
                config.endpoints.sessions,
                eventMetadata
            )
            launchMultiProcessService(scenarioParams) {
                Bugsnag.notify(generateException())
            }
        } else {
            Bugsnag.notify(generateException())
        }
    }

    /**
     * Writes user/device information to the legacy SharedPreferences location to test a migration.
     */
    @SuppressLint("ApplySharedPref")
    private fun persistLegacyPrefs() {
        val prefs = context.getSharedPreferences("com.bugsnag.android", Context.MODE_PRIVATE)

        if (isRunningFromBackgroundService()) {
            prefs.edit()
                .putString("install.iud", "267160a7-5cf2-42d4-be21-969f1573ecb0")
                .putString("user.id", "4")
                .putString("user.name", "SharedPrefMigrationScenario")
                .putString("user.email", "4@example.com")
                .commit()
        } else {
            prefs.edit()
                .putString("install.iud", "267160a7-5cf2-42d4-be21-969f1573ecb0")
                .putString("user.id", "3")
                .putString("user.name", "SharedPrefMigrationScenario")
                .putString("user.email", "3@example.com")
                .commit()
        }
    }
}
