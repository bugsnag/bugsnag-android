package com.bugsnag.android.kotlin

import android.support.test.annotation.UiThreadTest
import android.support.test.filters.LargeTest
import android.support.test.rule.ActivityTestRule
import android.support.test.runner.AndroidJUnit4
import com.bugsnag.android.Callback
import com.bugsnag.android.JsonStream
import com.bugsnag.android.Report
import com.bugsnag.android.example.ExampleActivity
import junit.framework.Assert.assertEquals
import junit.framework.Assert.assertNotNull
import org.json.JSONException
import org.json.JSONObject
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException
import java.io.StringWriter

@RunWith(AndroidJUnit4::class)
@LargeTest
class CrashReportTest {

    @Rule
    var activityTestRule = ActivityTestRule(ExampleActivity::class.java)

    private var report: Report? = null

    @Test
    @UiThreadTest
    @Throws(Exception::class)
    fun checkAppLaunches() {
        // send report via activity
        val activity = activityTestRule.activity
        assertNotNull(activity)
        activity.sendErrorWithCallback(Callback {
            this.report = it
        })

        // validate error report
        assertNotNull(report)
        val json = getJson(report!!)

        assertEquals("066f5ad3590596f9aa8d601ea89af845", json.getString("apiKey"))
        assertEquals(3, json.length())

        val event = json.getJSONArray("events").getJSONObject(0)
        assertNotNull(event)
        assertEquals("ExampleActivity", event.getString("context"))

        val exceptions = event.getJSONArray("exceptions")
        assertEquals(1, exceptions.length())
        assertNotNull(exceptions.getJSONObject(0))
    }

    @Throws(IOException::class, JSONException::class)
    private fun getJson(streamable: JsonStream.Streamable): JSONObject {
        val writer = StringWriter()
        val jsonStream = JsonStream(writer)
        streamable.toStream(jsonStream)
        return JSONObject(writer.toString())
    }

}
