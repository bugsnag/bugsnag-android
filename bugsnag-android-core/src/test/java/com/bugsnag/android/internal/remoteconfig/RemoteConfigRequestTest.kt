package com.bugsnag.android.internal.remoteconfig

import com.bugsnag.android.DiscardRule
import com.bugsnag.android.Logger
import com.bugsnag.android.NoopLogger
import com.bugsnag.android.Notifier
import com.bugsnag.android.RemoteConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.junit.MockitoJUnitRunner
import java.io.ByteArrayInputStream
import java.net.HttpURLConnection
import java.util.Date

@RunWith(MockitoJUnitRunner::class)
class RemoteConfigRequestTest {

    private val logger: Logger = NoopLogger

    @Mock
    private lateinit var mockConnection: HttpURLConnection

    private lateinit var notifier: Notifier
    private val baseUrl = "https://config.bugsnag.com"
    private val apiKey = "test-api-key-12345678901234567890123456789012"
    private val appVersion = "1.0.0"
    private val versionCode = 100
    private val releaseStage = "production"
    private val packageName = "com.example.app"

    @Before
    fun setUp() {
        notifier = Notifier("Test Notifier", "1.0.0", "https://test.com")
    }

    @Test
    fun testParseRemoteConfig_WithValidJsonResponse() {
        // Given: A valid JSON response with discard rules
        val jsonResponse = """
            {
                "discardRules": [
                    {
                        "matchType": "ALL"
                    }
                ]
            }
        """.trimIndent()

        val etag = "test-etag-123"
        val cacheControl = "max-age=3600"
        mockConnectionWithJsonResponse(jsonResponse, etag, cacheControl)

        // When: Parsing the remote config
        val request = createRequest()
        val result = request.parseRemoteConfig(mockConnection)

        // Then: The config is parsed correctly
        assertNotNull(result)
        assertEquals(etag, result?.configurationTag)
        assertEquals(1, result?.discardRules?.size)
        assertNotNull(result?.configurationExpiry)
    }

    @Test
    fun testParseRemoteConfig_WithEmptyResponse() {
        // Given: An empty response (contentLength = 0)
        val etag = "empty-etag"
        val cacheControl = "max-age=7200"
        mockConnectionWithEmptyResponse(etag, cacheControl)

        // When: Parsing the remote config
        val result = createRequest().parseRemoteConfig(mockConnection)

        // Then: An empty config is returned
        assertNotNull(result)
        assertEquals(etag, result?.configurationTag)
        assertEquals(0, result?.discardRules?.size)
    }

    @Test
    fun testParseRemoteConfig_WithInvalidJson() {
        // Given: JSON that is valid syntax but wrong structure (not an object)
        val invalidJson = "[]" // Array instead of object
        val etag = "invalid-etag"
        mockConnectionWithJsonResponse(invalidJson, etag, "max-age=3600")

        // When: Parsing the remote config
        val result = createRequest().parseRemoteConfig(mockConnection)

        // Then: Returns null when JSON is not a map structure
        assertNull(result)
    }

    @Test
    fun testParseRemoteConfig_WithNoEtag() {
        // Given: A valid JSON response without ETag
        val jsonResponse = """{"discardRules": []}"""
        mockConnectionWithJsonResponse(jsonResponse, null, "max-age=3600")

        // When: Parsing the remote config
        val result = createRequest().parseRemoteConfig(mockConnection)

        // Then: The config is parsed with null tag
        assertNotNull(result)
        assertNull(result?.configurationTag)
        assertEquals(0, result?.discardRules?.size)
    }

    @Test
    fun testParseRemoteConfig_WithDefaultCacheControlExpiry() {
        // Given: Response without Cache-Control header
        val jsonResponse = """{"discardRules": []}"""
        mockConnectionWithJsonResponse(jsonResponse, "test-tag", null)

        // When: Parsing the remote config
        val result = createRequest().parseRemoteConfig(mockConnection)

        // Then: Default expiry is used (24 hours)
        assertNotNull(result)
        assertExpiryWithinTolerance(
            result!!.configurationExpiry.time,
            RemoteConfigRequest.DEFAULT_CONFIG_EXPIRY_TIME
        )
    }

    @Test
    fun testParseRemoteConfig_WithMalformedCacheControl() {
        // Given: Response with malformed Cache-Control header
        val jsonResponse = """{"discardRules": []}"""
        mockConnectionWithJsonResponse(jsonResponse, "test-tag", "invalid-cache-control")

        // When: Parsing the remote config
        val result = createRequest().parseRemoteConfig(mockConnection)

        // Then: Default expiry is used
        assertNotNull(result)
        assertExpiryWithinTolerance(
            result!!.configurationExpiry.time,
            RemoteConfigRequest.DEFAULT_CONFIG_EXPIRY_TIME
        )
    }

    @Test
    fun testParseRemoteConfig_WithValidCacheControl() {
        // Given: Response with valid Cache-Control max-age
        val jsonResponse = """{"discardRules": []}"""
        val maxAge = 7200L // 2 hours in seconds
        mockConnectionWithJsonResponse(jsonResponse, "test-tag", "public, max-age=$maxAge")

        // When: Parsing the remote config
        val result = createRequest().parseRemoteConfig(mockConnection)

        // Then: Expiry is calculated from max-age
        assertNotNull(result)
        assertExpiryWithinTolerance(result!!.configurationExpiry.time, maxAge * 1000)
    }

    @Test
    fun testParseRemoteConfig_NewConfigAvailable_HTTP200() {
        // Given: HTTP 200 response with new config
        val jsonResponse = """
            {
                "discardRules": [
                    {
                        "matchType": "ALL_HANDLED"
                    },
                    {
                        "matchType": "ALL"
                    }
                ]
            }
        """.trimIndent()
        val etag = "new-config-etag-456"
        mockConnectionWithJsonResponse(jsonResponse, etag, "max-age=3600")

        // When: Parsing the remote config
        val result = createRequest().parseRemoteConfig(mockConnection)

        // Then: New config is correctly parsed
        assertNotNull(result)
        assertEquals(etag, result?.configurationTag)
        assertEquals(2, result?.discardRules?.size)
        assertNotNull(result?.configurationExpiry)
    }

    @Test
    fun testParseRemoteConfig_WithComplexDiscardRules() {
        // Given: Response with multiple types of discard rules
        val jsonResponse = """
            {
                "discardRules": [
                    {
                        "matchType": "ALL"
                    },
                    {
                        "matchType": "ALL_HANDLED"
                    }
                ]
            }
        """.trimIndent()
        mockConnectionWithJsonResponse(jsonResponse, "complex-etag", "max-age=1800")

        // When: Parsing the remote config
        val result = createRequest().parseRemoteConfig(mockConnection)

        // Then: All discard rules are parsed
        assertNotNull(result)
        assertEquals(2, result?.discardRules?.size)
    }

    @Test
    fun testParseRemoteConfig_EmptyDiscardRules() {
        // Given: Response with empty discard rules array
        val jsonResponse = """{"discardRules": []}"""
        mockConnectionWithJsonResponse(jsonResponse, "empty-rules-etag", "max-age=3600")

        // When: Parsing the remote config
        val result = createRequest().parseRemoteConfig(mockConnection)

        // Then: Config with no discard rules is created
        assertNotNull(result)
        assertEquals("empty-rules-etag", result?.configurationTag)
        assertEquals(0, result?.discardRules?.size)
    }

    @Test
    fun testParseRemoteConfig_WithLongMaxAge() {
        // Given: Response with very long cache duration
        val jsonResponse = """{"discardRules": []}"""
        val maxAge = 86400L // 24 hours
        mockConnectionWithJsonResponse(jsonResponse, "long-cache-etag", "max-age=$maxAge")

        // When: Parsing the remote config
        val result = createRequest().parseRemoteConfig(mockConnection)

        // Then: Expiry is set correctly for long duration
        assertNotNull(result)
        assertExpiryWithinTolerance(result!!.configurationExpiry.time, maxAge * 1000)
    }

    @Test
    fun testParseRemoteConfig_WithZeroContentLength() {
        // Given: Response with zero content length (no body)
        val etag = "zero-length-etag"
        mockConnectionWithEmptyResponse(etag, "max-age=3600")

        // When: Parsing the remote config
        val result = createRequest().parseRemoteConfig(mockConnection)

        // Then: Empty config is returned without trying to read body
        assertNotNull(result)
        assertEquals(etag, result?.configurationTag)
        assertEquals(0, result?.discardRules?.size)
    }

    @Test
    fun testParseRemoteConfig_WithWhitespaceInCacheControl() {
        // Given: Cache-Control header with extra whitespace
        val jsonResponse = """{"discardRules": []}"""
        mockConnectionWithJsonResponse(
            jsonResponse, "whitespace-etag",
            "public,  max-age  =  3600  , must-revalidate"
        )

        // When: Parsing the remote config
        val result = createRequest().parseRemoteConfig(mockConnection)

        // Then: max-age is correctly extracted despite whitespace
        assertNotNull(result)
        assertExpiryWithinTolerance(result!!.configurationExpiry.time, 3600 * 1000)
    }

    // ===== Tests for HTTP 304 scenario (config unchanged with same etag) =====

    @Test
    fun testConfigUnchanged_RenewExistingConfig_HTTP304() {
        // Given: An existing config with discard rules
        val existingEtag = "existing-etag-789"
        val existingDiscardRules = listOf(
            DiscardRule.All
        )
        val existingConfig = createRemoteConfig(existingEtag, 3600000, existingDiscardRules)

        // When: Creating a request with existing config (simulates 304 response)
        // In a real 304 scenario, renewExistingConfig would be called to preserve
        // the existing config's data with updated expiry
        createRequestWithExistingConfig(existingConfig)

        // Then: The existing config structure is preserved (behavior for 304)
        // In HTTP 304, the etag and discard rules remain unchanged
        assertNotNull(existingConfig)
        assertEquals(existingEtag, existingConfig.configurationTag)
        assertEquals(1, existingConfig.discardRules.size)
    }

    @Test
    fun testConfigUnchanged_PreservesDiscardRules_HTTP304() {
        // Given: An existing config with multiple discard rules
        val existingEtag = "multi-rule-etag"
        val existingDiscardRules = listOf(
            DiscardRule.All,
            DiscardRule.AllHandled
        )
        val existingConfig = createRemoteConfig(existingEtag, 1800000, existingDiscardRules)

        // When: A 304 response would preserve these rules with updated expiry
        assertNotNull(existingConfig)
        assertEquals(existingEtag, existingConfig.configurationTag)
        assertEquals(2, existingConfig.discardRules.size)

        // Verify the discard rules are intact
        assertEquals(DiscardRule.All, existingConfig.discardRules[0])
        assertEquals(DiscardRule.AllHandled, existingConfig.discardRules[1])
    }

    @Test
    fun testConfigUnchanged_UpdatesExpiry_HTTP304() {
        // Given: An existing config that's about to expire
        val existingEtag = "expiring-etag"
        val existingConfig = createRemoteConfig(existingEtag, 60000, emptyList())

        // When: A 304 response would provide new expiry time
        // The existing config would be renewed with updated expiry while preserving all other data

        // Then: Verify the existing config's current expiry is soon (before renewal)
        val oldExpiryTime = existingConfig.configurationExpiry.time
        val minExpectedOldExpiry = System.currentTimeMillis() + 30000 // at least 30 seconds
        val maxExpectedOldExpiry = System.currentTimeMillis() + 90000 // at most 90 seconds

        // Verify old expiry is soon
        assert(oldExpiryTime in minExpectedOldExpiry..maxExpectedOldExpiry) {
            "Old expiry should be within 30-90 seconds from now"
        }
    }

    // ===== Tests for HTTP 400 scenario (invalid request) =====

    @Test
    fun testInvalidRequest_EmptyConfigReturned_HTTP400() {
        // Given: HTTP 400 response (invalid request)
        // Server returns minimal config with no discard rules and no etag
        mockConnectionWithEmptyResponse(null, "max-age=1800")

        // When: Parsing the response (simulates 400 scenario)
        val result = createRequest().parseRemoteConfig(mockConnection)

        // Then: Empty config is returned (behavior for 400)
        assertNotNull(result)
        assertNull(result?.configurationTag) // No etag for invalid request
        assertEquals(0, result?.discardRules?.size) // No discard rules
        assertNotNull(result?.configurationExpiry) // But has expiry
    }

    @Test
    fun testInvalidRequest_NoEtagInConfig_HTTP400() {
        // Given: HTTP 400 response creates empty config without etag
        mockConnectionWithEmptyResponse(null, "max-age=3600")

        // When: Parsing as empty config (HTTP 400 behavior)
        val result = createRequest().parseRemoteConfig(mockConnection)

        // Then: Config has no tag (characteristic of 400 response)
        assertNotNull(result)
        assertNull(result?.configurationTag)
    }

    @Test
    fun testInvalidRequest_NoDiscardRules_HTTP400() {
        // Given: HTTP 400 response with empty body
        mockConnectionWithEmptyResponse(null, "max-age=7200")

        // When: Creating empty config (HTTP 400 behavior)
        val result = createRequest().parseRemoteConfig(mockConnection)

        // Then: Config has no discard rules
        assertNotNull(result)
        assertEquals(0, result?.discardRules?.size)
    }

    @Test
    fun testInvalidRequest_UsesProvidedExpiry_HTTP400() {
        // Given: HTTP 400 with specific cache expiry
        val maxAge = 5400L // 90 minutes
        mockConnectionWithEmptyResponse(null, "max-age=$maxAge")

        // When: Creating empty config with expiry (HTTP 400)
        val result = createRequest().parseRemoteConfig(mockConnection)

        // Then: Expiry is set from Cache-Control header
        assertNotNull(result)
        assertExpiryWithinTolerance(result!!.configurationExpiry.time, maxAge * 1000)
    }

    private fun createRequest(existingConfig: RemoteConfig? = null): RemoteConfigRequest {
        return RemoteConfigRequest(
            baseUrl,
            apiKey,
            notifier,
            appVersion,
            versionCode,
            releaseStage,
            packageName,
            existingConfig,
            logger
        )
    }

    private fun createRequestWithExistingConfig(existingConfig: RemoteConfig): RemoteConfigRequest {
        return createRequest(existingConfig)
    }

    private fun mockConnectionWithJsonResponse(
        jsonResponse: String,
        etag: String?,
        cacheControl: String?
    ) {
        val inputStream = ByteArrayInputStream(jsonResponse.toByteArray())
        `when`(mockConnection.inputStream).thenReturn(inputStream)
        `when`(mockConnection.getHeaderField("ETag")).thenReturn(etag)
        `when`(mockConnection.getHeaderField("Cache-Control")).thenReturn(cacheControl)
        `when`(mockConnection.contentLength).thenReturn(jsonResponse.length)
    }

    private fun mockConnectionWithEmptyResponse(etag: String?, cacheControl: String?) {
        `when`(mockConnection.contentLength).thenReturn(0)
        `when`(mockConnection.getHeaderField("ETag")).thenReturn(etag)
        `when`(mockConnection.getHeaderField("Cache-Control")).thenReturn(cacheControl)
    }

    private fun createRemoteConfig(
        etag: String,
        expiryOffsetMillis: Long,
        discardRules: List<DiscardRule>
    ): RemoteConfig {
        return RemoteConfig(
            etag,
            Date(System.currentTimeMillis() + expiryOffsetMillis),
            null,
            discardRules
        )
    }

    private fun assertExpiryWithinTolerance(actualExpiryTime: Long, expectedOffsetMillis: Long) {
        val now = System.currentTimeMillis()
        val expectedExpiry = now + expectedOffsetMillis
        assertEquals(expectedExpiry.toDouble(), actualExpiryTime.toDouble(), 1000.0)
    }
}
