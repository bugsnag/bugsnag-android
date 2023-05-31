package com.bugsnag.android

import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File

class SessionFilenameTest {
    private var config = BugsnagTestUtils.generateImmutableConfig()

    @Test
    fun getSessionDetailsFromV3FileName() {
        val apiKey = "TEST APIKEY"
        val fileName = SessionFilenameInfo(
            apiKey,
            1504255147933,
            "my-uuid-uuuuuuuuuuuuuuuuuuuuuuuuuuuu"
        ).encode()
        val file = File(fileName)
        val sessionInfo = SessionFilenameInfo.fromFile(file, defaultApiKey = config.apiKey)

        assertEquals(
            "TEST APIKEY_my-uuid-uuuuuuuuuuuuuuuuuuuuuuuuuuuu1504255147933_v3.json",
            fileName
        )
        assertEquals("TEST APIKEY", sessionInfo.apiKey)
        assertEquals(1504255147933, sessionInfo.timestamp)
        assertEquals("my-uuid-uuuuuuuuuuuuuuuuuuuuuuuuuuuu", sessionInfo.uuid)
    }

    @Test
    fun getFileDetailsFromV2FileName() {
        val file = File("my-uuid-uuuuuuuuuuuuuuuuuuuuuuuuuuuu1504255147933_v2.json")
        val sessionInfo = SessionFilenameInfo.fromFile(file, defaultApiKey = config.apiKey)
        assertEquals("my-uuid-uuuuuuuuuuuuuuuuuuuuuuuuuuuu", sessionInfo.uuid)
        assertEquals(1504255147933, sessionInfo.timestamp)
        assertEquals("5d1ec5bd39a74caa1267142706a7fb21", sessionInfo.apiKey)
    }

    @Test
    fun getSessionApiKeyFromV3FileNameWithoutApiKey() {
        val fileName = SessionFilenameInfo(
            "",
            1504255147933,
            "my-uuid-uuuuuuuuuuuuuuuuuuuuuuuuuuuu"
        ).encode()

        val file = File(fileName)
        val sessionInfo = SessionFilenameInfo.fromFile(file, defaultApiKey = config.apiKey)
        assertEquals(config.apiKey, sessionInfo.apiKey)
        assertEquals("my-uuid-uuuuuuuuuuuuuuuuuuuuuuuuuuuu", sessionInfo.uuid)
        assertEquals(1504255147933, sessionInfo.timestamp)
    }

    @Test
    fun getSessionTimeStampFromV3FileNameWithoutUuid() {
        val apiKey = "TEST APIKEY"
        val fileName = SessionFilenameInfo(
            apiKey,
            1504255147933,
            ""
        ).encode()

        val file = File(fileName)
        val sessionInfo = SessionFilenameInfo.fromFile(file, defaultApiKey = config.apiKey)
        assertEquals("TEST APIKEY", sessionInfo.apiKey)
        assertEquals(1504255147933, sessionInfo.timestamp)
        assertEquals("", sessionInfo.uuid)
    }

    @Test
    fun getSessionUuidAndTimeStampFromV2FileNameWithoutUuid() {
        val file = File("1504255147933_v2.json")
        val sessionInfo = SessionFilenameInfo.fromFile(file, defaultApiKey = config.apiKey)
        assertEquals("", sessionInfo.uuid)
        assertEquals(1504255147933, sessionInfo.timestamp)
    }

    @Test
    fun getSessionTimeStampFromV3FileNameWithoutUuidAndApiKey() {
        val fileName = SessionFilenameInfo(
            "",
            1504255147933,
            ""
        ).encode()

        val file = File(fileName)
        val sessionInfo = SessionFilenameInfo.fromFile(file, defaultApiKey = config.apiKey)
        assertEquals(config.apiKey, sessionInfo.apiKey)
        assertEquals(1504255147933, sessionInfo.timestamp)
        assertEquals("", sessionInfo.uuid)
    }

    @Test
    fun getSessionUuidFromV2FileNameWithoutUuidAndApiKey() {
        val file = File("1504255147933_v2.json")
        val sessionInfo = SessionFilenameInfo.fromFile(file, defaultApiKey = config.apiKey)
        assertEquals("", sessionInfo.uuid)
        assertEquals(config.apiKey, sessionInfo.apiKey)
        assertEquals(1504255147933, sessionInfo.timestamp)
    }

    @Test
    fun getSessionTimeStampFromV3FileNameWithoutTimeStamp() {
        val file = File("_my-uuid-uuuuuuuuuuuuuuuuuuuuuuuuuuuu_v3.json")
        val sessionInfo = SessionFilenameInfo.fromFile(file, defaultApiKey = config.apiKey)
        assertEquals(config.apiKey, sessionInfo.apiKey)
        assertEquals(-1, sessionInfo.timestamp)
        assertEquals("my-uuid-uuuuuuuuuuuuuuuuuuuuuuuuuuuu", sessionInfo.uuid)
    }

    @Test
    fun getSessionUuidAndTimeStampFromV2FileNameWithoutTimeStamp() {
        val file = File("my-uuid-uuuuuuuuuuuuuuuuuuuuuuuuuuuu_v2.json")
        val sessionInfo = SessionFilenameInfo.fromFile(file, defaultApiKey = config.apiKey)
        assertEquals(-1, sessionInfo.timestamp)
        assertEquals("my-uuid-uuuuuuuuuuuuuuuuuuuuuuuuuuuu", sessionInfo.uuid)
    }

    @Test
    fun getSessionUuidAndTimeStampFromV2FileNameWithNoTimeStampAndNoUuid() {
        val file = File("_v2.json")
        val sessionInfo = SessionFilenameInfo.fromFile(file, defaultApiKey = config.apiKey)
        assertEquals(-1, sessionInfo.timestamp)
        assertEquals("", sessionInfo.uuid)
    }

    @Test
    fun getSessionDetailsFromV2FileNameWithNoTimeStampAndNoUuidAndApiKey() {
        val file = File("_v3.json")
        val sessionInfo = SessionFilenameInfo.fromFile(file, defaultApiKey = config.apiKey)
        assertEquals(-1, sessionInfo.timestamp)
        assertEquals("", sessionInfo.uuid)
        assertEquals(config.apiKey, sessionInfo.apiKey)
    }
}
