package com.bugsnag.android

import com.bugsnag.android.BugsnagTestUtils.streamableToJsonArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File

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
        val fileContents = javaClass.classLoader!!.getResource("rx_error.json")!!.readText()
        val fixtureFile = File.createTempFile("rx_error", ".json")
        fixtureFile.writeText(fileContents)
        return ErrorReader.readError(Configuration("key"), fixtureFile)
    }
}
