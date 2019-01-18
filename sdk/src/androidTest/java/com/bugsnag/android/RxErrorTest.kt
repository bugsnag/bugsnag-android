package com.bugsnag.android

import android.support.test.InstrumentationRegistry
import com.bugsnag.android.BugsnagTestUtils.streamableToJsonArray
import com.bugsnag.android.test.R
import org.json.JSONObject
import org.junit.Assert.*

import org.junit.Test

import java.io.File
import java.io.FileOutputStream

class RxErrorTest {

    @Test
    fun loadRxError() {
        val error = loadErrorFromFile()!!
        val jsonArray = streamableToJsonArray(error.exceptions)

        with(jsonArray) {
            assertEquals(2, length())
            validateRootThrowable(getJSONObject(0))
            validateCauseThrowable(getJSONObject(1))
        }
    }

    private fun validateRootThrowable(rootExc: JSONObject) {
        assertEquals(
            "io.reactivex.exceptions.OnErrorNotImplementedException",
            rootExc.getString("errorClass")
        )
        assertEquals(
            "The exception was not handled due to missing onError handler " +
                "in the subscribe() method call. Further reading: https://github.com/ReactiveX/" +
                "RxJava/wiki/Error-Handling | java.lang.RuntimeException: Whoops!",
            rootExc.getString("message")
        )
        assertEquals("custom", rootExc.getString("type"))
        assertEquals(32, rootExc.getJSONArray("stacktrace").length())
    }

    private fun validateCauseThrowable(causeExc: JSONObject) {
        assertEquals("java.lang.RuntimeException", causeExc.getString("errorClass"))
        assertEquals("Whoops!", causeExc.getString("message"))
        assertEquals("custom", causeExc.getString("type"))
        assertEquals(29, causeExc.getJSONArray("stacktrace").length())
    }

    private fun loadErrorFromFile(): Error? {
        val input = InstrumentationRegistry.getContext().resources
            .openRawResource(R.raw.rx_error)
        val fixtureFile = File.createTempFile("rx_error", ".json")
        input.copyTo(FileOutputStream(fixtureFile))
        return ErrorReader.readError(Configuration("key"), fixtureFile)
    }
}
