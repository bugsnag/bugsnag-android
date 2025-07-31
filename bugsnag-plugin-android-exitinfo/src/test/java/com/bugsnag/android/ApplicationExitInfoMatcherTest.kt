package com.bugsnag.android

import android.app.ApplicationExitInfo
import com.bugsnag.android.test.mockAppExitInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when` as whenever

class ApplicationExitInfoMatcherTest {
    private val exitInfoList = listOf(
        mockAppExitInfo(1234, 1000L, ApplicationExitInfo.REASON_ANR),
        mockAppExitInfo(2345, 2000L, ApplicationExitInfo.REASON_CRASH),
        mockAppExitInfo(3456, 3000L, ApplicationExitInfo.REASON_ANR),
        mockAppExitInfo(4567, 4000L, ApplicationExitInfo.REASON_ANR),
    )

    @Test
    fun matchExitInfoWithNullSessionReturnsNull() {
        val matcher = ApplicationExitInfoMatcher(exitInfoList)
        val event = mock(Event::class.java)
        whenever(event.session).thenReturn(null)

        val result = matcher.matchExitInfo(event)

        assertNull(result)
    }

    @Test
    fun matchExitInfoWithMatchingSessionIdReturnsCorrectExitInfo() {
        val sessionId = "test-session-id"
        val sessionBytes = sessionId.toByteArray()

        // Create a custom exit info with matching session bytes
        val matchingExitInfo = mockAppExitInfo(
            5678,
            5000L,
            ApplicationExitInfo.REASON_CRASH,
            sessionBytes
        )

        val customExitInfoList = exitInfoList + matchingExitInfo
        val matcher = ApplicationExitInfoMatcher(customExitInfoList)

        val session = mock(Session::class.java)
        whenever(session.id).thenReturn(sessionId)

        val event = mock(Event::class.java)
        whenever(event.session).thenReturn(session)

        val result = matcher.matchExitInfo(event)

        assertEquals(matchingExitInfo, result)
    }

    @Test
    fun matchExitInfoFallsBackToPidMatchingWhenSessionIdNotFound() {
        val sessionId = "non-matching-session-id"
        val previousState = ExitInfoPluginStore.PersistentState(1234, 0, emptySet())
        val matcher = ApplicationExitInfoMatcher(exitInfoList, previousState)

        val session = mock(Session::class.java)
        whenever(session.id).thenReturn(sessionId)

        val event = mock(Event::class.java)
        whenever(event.session).thenReturn(session)

        val result = matcher.matchExitInfo(event)
        assertEquals(exitInfoList[0], result)
    }

    @Test
    fun matchExitInfoReturnsNullWhenNeitherSessionIdNorPidMatches() {
        val sessionId = "non-matching-session-id"
        val previousState = ExitInfoPluginStore.PersistentState(9999, 0, emptySet())
        val matcher = ApplicationExitInfoMatcher(exitInfoList, previousState)

        val session = mock(Session::class.java)
        whenever(session.id).thenReturn(sessionId)

        val event = mock(Event::class.java)
        whenever(event.session).thenReturn(session)

        val result = matcher.matchExitInfo(event)
        assertNull(result)
    }

    @Test
    fun findExitInfoBySessionIdWithMatchingBytesReturnsCorrectExitInfo() {
        val sessionId = "test-session-id"
        val sessionBytes = sessionId.toByteArray()

        val matchingExitInfo = mockAppExitInfo(
            5678,
            5000L,
            ApplicationExitInfo.REASON_CRASH,
            sessionBytes
        )

        val customExitInfoList = listOf(matchingExitInfo)
        val matcher = ApplicationExitInfoMatcher(customExitInfoList)

        val result = matcher.findExitInfoBySessionId(customExitInfoList, sessionBytes)
        assertEquals(matchingExitInfo, result)
    }

    @Test
    fun findExitInfoBySessionIdWithNonMatchingBytesReturnsNull() {
        val sessionBytes = "non-matching-session-id".toByteArray()
        val matcher = ApplicationExitInfoMatcher(exitInfoList)

        val result = matcher.findExitInfoBySessionId(exitInfoList, sessionBytes)
        assertNull(result)
    }

    @Test
    fun findExitInfoByPidWithMatchingPidReturnsCorrectExitInfo() {
        val previousState = ExitInfoPluginStore.PersistentState(1234, 0, emptySet())
        val matcher = ApplicationExitInfoMatcher(exitInfoList, previousState)

        val result = matcher.findExitInfoByPid(exitInfoList)
        assertEquals(exitInfoList[0], result)
    }

    @Test
    fun findExitInfoByPidWithNonMatchingPidReturnsNull() {
        val previousState = ExitInfoPluginStore.PersistentState(9999, 0, emptySet())
        val matcher = ApplicationExitInfoMatcher(exitInfoList, previousState)

        val result = matcher.findExitInfoByPid(exitInfoList)
        assertNull(result)
    }

    @Test
    fun findExitInfoByPidWithNullPreviousStateReturnsNull() {
        val matcher = ApplicationExitInfoMatcher(exitInfoList)
        val result = matcher.findExitInfoByPid(exitInfoList)
        assertNull(result)
    }

    @Test
    fun whenMultipleExitInfosHaveSameSessionIdOnlyFirstMatchIsReturned() {
        val sessionId = "duplicate-session-id"
        val sessionBytes = sessionId.toByteArray()

        // Create multiple exit infos with the same session bytes
        val exitInfo1 = mockAppExitInfo(
            5678,
            5000L,
            ApplicationExitInfo.REASON_CRASH,
            sessionBytes
        )

        val exitInfo2 = mockAppExitInfo(
            6789,
            6000L,
            ApplicationExitInfo.REASON_ANR,
            sessionBytes
        )

        val customExitInfoList = listOf(exitInfo1, exitInfo2)
        val matcher = ApplicationExitInfoMatcher(customExitInfoList)

        val session = mock(Session::class.java)
        whenever(session.id).thenReturn(sessionId)

        val event = mock(Event::class.java)
        whenever(event.session).thenReturn(session)

        val result = matcher.matchExitInfo(event)
        assertEquals(exitInfo1, result)
    }

    @Test
    fun emptyExitInfoListReturnsNullForAnyMatchAttempt() {
        val sessionId = "test-session-id"
        val sessionBytes = sessionId.toByteArray()
        val matcher = ApplicationExitInfoMatcher(emptyList())

        val session = mock(Session::class.java)
        whenever(session.id).thenReturn(sessionId)

        val event = mock(Event::class.java)
        whenever(event.session).thenReturn(session)

        assertNull(matcher.matchExitInfo(event))
        assertNull(matcher.findExitInfoBySessionId(emptyList(), sessionBytes))
        assertNull(matcher.findExitInfoByPid(emptyList()))
    }
}
