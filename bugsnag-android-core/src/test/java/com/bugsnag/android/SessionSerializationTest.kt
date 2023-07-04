package com.bugsnag.android

import com.bugsnag.android.BugsnagTestUtils.generateApp
import com.bugsnag.android.BugsnagTestUtils.generateDevice
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.Parameterized.Parameter
import org.junit.runners.Parameterized.Parameters
import java.util.Date

@RunWith(Parameterized::class)
internal class SessionSerializationTest {

    companion object {
        private val notifier = Notifier("AndroidBugsnagNotifier", "9.9.9", "https://bugsnag.com")

        @JvmStatic
        @Parameters
        fun testCases(): Collection<Pair<Any, String>> {
            val session = Session("123", Date(0), User(null, null, null), 1, 0, notifier, NoopLogger, "BUGSNAG_API_KEY")
            session.app = generateApp()
            session.device = generateDevice()
            return generateSerializationTestCases("session", session)
        }
    }

    @Parameter
    lateinit var testCase: Pair<Session, String>

    @Test
    fun testJsonSerialisation() = verifyJsonMatches(testCase.first, testCase.second)
}
