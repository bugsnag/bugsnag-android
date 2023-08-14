package com.bugsnag.android

import com.bugsnag.android.internal.ImmutableConfig
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.util.EnumSet
import java.util.regex.Pattern

@Suppress("UNCHECKED_CAST")
internal class TestData {
    @Throws(IOException::class)
    fun generateConfig(): ImmutableConfig? {
        return lazy {
            try {
                return@lazy Files.createTempDirectory("foo").toFile()
            } catch (ignored: IOException) {
                return@lazy null
            }
        }.let {
            ImmutableConfig(
                "123456abcdeabcde",
                true,
                ErrorTypes(),
                true,
                ThreadSendPolicy.ALWAYS,
                setOf(Pattern.compile("com.example.DiscardClass")),
                setOf("production"),
                setOf("com.example"),
                HashSet(listOf(BreadcrumbType.MANUAL)),
                EnumSet.of(Telemetry.INTERNAL_ERRORS, Telemetry.USAGE),
                "production",
                "builduuid-123",
                "1.4.3",
                55,
                "android",
                DefaultDelivery(null, "myApiKey", 10000, NoopLogger),
                EndpointConfiguration(),
                true,
                55,
                NoopLogger,
                22,
                32,
                32,
                1000,
                it as Lazy<File>,
                true,
                true,
                null,
                null,
                setOf(Pattern.compile(".*password.*"))
            )
        }
    }
}
