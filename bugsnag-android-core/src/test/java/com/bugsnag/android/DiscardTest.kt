package com.bugsnag.android

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.lang.RuntimeException
import java.util.regex.Pattern

/**
 * Verifies the logic for discarding automatically captured errors/sessions/breadcrumbs.
 */
class DiscardTest {

    private lateinit var config: Configuration

    @Before
    fun setUp() {
        config = BugsnagTestUtils.generateConfiguration()
    }

    @Test
    fun testShouldDiscardErrorUsingThrowable() {
        val exc = RuntimeException()

        // Should not discard if enabledReleaseStages is null
        config.enabledReleaseStages = null
        var cfg = BugsnagTestUtils.convert(config)
        assertFalse(cfg.shouldDiscardError(exc))

        // Should discard if outside enabledReleaseStages
        config.enabledReleaseStages = setOf("prod")
        config.releaseStage = "dev"
        cfg = BugsnagTestUtils.convert(config)
        assertTrue(cfg.shouldDiscardError(exc))

        // Should not discard if inside enabledReleaseStages
        config.enabledReleaseStages = setOf("prod")
        config.releaseStage = "prod"
        cfg = BugsnagTestUtils.convert(config)
        assertFalse(cfg.shouldDiscardError(exc))

        // Should not discard if outside discard classes
        config.discardClasses = setOf(Pattern.compile("UnwantedError"))
        cfg = BugsnagTestUtils.convert(config)
        assertFalse(cfg.shouldDiscardError(exc))

        // Should discard if inside discard classes
        config.discardClasses = setOf(Pattern.compile("java.lang.RuntimeException"))
        cfg = BugsnagTestUtils.convert(config)
        assertTrue(cfg.shouldDiscardError(exc))
    }

    @Test
    fun testShouldDiscardErrorUsingClz() {
        // Should not discard if enabledReleaseStages is null
        config.enabledReleaseStages = null
        var cfg = BugsnagTestUtils.convert(config)
        assertFalse(cfg.shouldDiscardError("MyError"))

        // Should discard if outside enabledReleaseStages
        config.enabledReleaseStages = setOf("prod")
        config.releaseStage = "dev"
        cfg = BugsnagTestUtils.convert(config)
        assertTrue(cfg.shouldDiscardError("MyError"))

        // Should not discard if inside enabledReleaseStages
        config.enabledReleaseStages = setOf("prod")
        config.releaseStage = "prod"
        cfg = BugsnagTestUtils.convert(config)
        assertFalse(cfg.shouldDiscardError("MyError"))

        // Should not discard if outside discard classes
        config.discardClasses = setOf(Pattern.compile("UnwantedError"))
        cfg = BugsnagTestUtils.convert(config)
        assertFalse(cfg.shouldDiscardError("MyError"))

        // Should discard if inside discard classes
        config.discardClasses = setOf(Pattern.compile("UnwantedError"))
        cfg = BugsnagTestUtils.convert(config)
        assertTrue(cfg.shouldDiscardError("UnwantedError"))
    }

    @Test
    fun testShouldDiscardSession() {
        // Should not discard if enabledReleaseStages is null
        config.enabledReleaseStages = null
        var cfg = BugsnagTestUtils.convert(config)
        assertFalse(cfg.shouldDiscardSession(false))

        // Should discard if outside enabledReleaseStages
        config.enabledReleaseStages = setOf("prod")
        config.releaseStage = "dev"
        cfg = BugsnagTestUtils.convert(config)
        assertTrue(cfg.shouldDiscardSession(false))

        // Should not discard if inside enabledReleaseStages
        config.enabledReleaseStages = setOf("prod")
        config.releaseStage = "prod"
        cfg = BugsnagTestUtils.convert(config)
        assertFalse(cfg.shouldDiscardSession(false))

        // Should not discard if autoTrack disabled and autoCapture == false
        config.autoTrackSessions = false
        cfg = BugsnagTestUtils.convert(config)
        assertFalse(cfg.shouldDiscardSession(false))

        // Should discard if autoTrack disabled and autoCapture == false
        config.autoTrackSessions = false
        cfg = BugsnagTestUtils.convert(config)
        assertTrue(cfg.shouldDiscardSession(true))
    }

    @Test
    fun testShouldDiscardBreadcrumb() {
        // Should not discard if enabledBreadcrumbTypes is null
        config.enabledBreadcrumbTypes = null
        var cfg = BugsnagTestUtils.convert(config)
        assertFalse(cfg.shouldDiscardBreadcrumb(BreadcrumbType.MANUAL))

        // Should discard if enabledBreadcrumbTypes is empty
        config.enabledBreadcrumbTypes = emptySet<BreadcrumbType>()
        cfg = BugsnagTestUtils.convert(config)
        assertTrue(cfg.shouldDiscardBreadcrumb(BreadcrumbType.MANUAL))

        // Should not discard if present in enabled types
        config.enabledBreadcrumbTypes = setOf(BreadcrumbType.MANUAL)
        cfg = BugsnagTestUtils.convert(config)
        assertFalse(cfg.shouldDiscardBreadcrumb(BreadcrumbType.MANUAL))

        // Should discard if not present in enabled types
        config.enabledBreadcrumbTypes = setOf(BreadcrumbType.ERROR)
        cfg = BugsnagTestUtils.convert(config)
        assertTrue(cfg.shouldDiscardBreadcrumb(BreadcrumbType.MANUAL))
    }
}
