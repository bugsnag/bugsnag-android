package com.bugsnag.android

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.bugsnag.android.BugsnagTestUtils.mapToJson
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class AppDataOverrideTest {

    private var appData: MutableMap<String, Any>? = null

    @Mock
    internal var client: Client? = null

    @Mock
    internal var sessionTracker: SessionTracker? = null

    @Before
    fun setUp() {
        val config = Configuration("api-key")
        config.appVersion = "1.2.3"
        config.releaseStage = "test-stage"

        val context = ApplicationProvider.getApplicationContext<Context>()
        val packageManager = context.packageManager
        val obj = AppData(context, packageManager, config, sessionTracker)
        this.appData = obj.appData
    }

    @Test
    fun testAppVersionOverride() {
        val appDataJson = mapToJson(appData)
        assertEquals("1.2.3", appDataJson.get("version"))
        assertEquals("test-stage", appDataJson.get("releaseStage"))
    }
}
