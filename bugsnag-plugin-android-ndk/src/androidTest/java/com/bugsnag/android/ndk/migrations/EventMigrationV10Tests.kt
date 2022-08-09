package com.bugsnag.android.ndk.migrations

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.fail
import org.junit.Test

/** Migration v10 added codeIdentifier to stack frames */
class EventMigrationV10Tests : EventMigrationTest() {

    @Test
    /** check notifier and api key, since they aren't included in event JSON */
    fun testMigrationPayloadInfo() {
        val infoFile = createTempFile()

        val info = migratePayloadInfo(infoFile.absolutePath)

        assertEquals(
            mapOf(
                "apiKey" to "5d1e5fbd39a74caa1200142706a90b20",
                "notifierName" to "Test Library",
                "notifierURL" to "https://example.com/test-lib",
                "notifierVersion" to "2.0.11"
            ),
            parseJSON(info)
        )
    }

    @Test
    fun testMigrateEventToLatest() {
        val eventFile = createTempFile()

        migrateEvent(eventFile.absolutePath)
        assertNotEquals(0, eventFile.length())

        val output = parseJSON(eventFile)

        assertEquals(
            "00000000000m0r3.61ee9e6e099d3dd7448f740d395768da6b2df55d5.m4g1c",
            output["context"]
        )
        assertEquals(
            "a1d34088a096987361ee9e6e099d3dd7448f740d395768da6b2df55d5160f33",
            output["groupingHash"]
        )
        assertEquals("info", output["severity"])

        // app
        assertEquals(
            mapOf(
                "binaryArch" to "mips",
                "buildUUID" to "1234-9876-adfe",
                "duration" to 81395165021L,
                "durationInForeground" to 81395165010L,
                "id" to "com.example.PhotoSnapPlus",
                "inForeground" to true,
                "isLaunching" to true,
                "releaseStage" to "リリース",
                "type" to "red",
                "version" to "2.0.52",
                "versionCode" to 8139512718L
            ),
            output["app"]
        )

        // breadcrumbs
        val crumbs = output["breadcrumbs"]
        if (crumbs is List<Any?>) {
            assertEquals(50, crumbs.size)
            crumbs.forEachIndexed { index, crumb ->
                assertEquals(
                    mapOf(
                        "type" to "state",
                        "name" to "mission $index",
                        "timestamp" to "2021-12-08T19:43:50.014Z",
                        "metaData" to mapOf(
                            "message" to "Now we know what they mean by 'advanced' tactical training."
                        )
                    ),
                    crumb
                )
            }
        } else {
            fail("breadcrumbs is not a list of crumb objects?!")
        }

        // device
        assertEquals(
            mapOf(
                "cpuAbi" to listOf("mipsx"),
                "id" to "ffffa",
                "locale" to "en_AU#Melbun",
                "jailbroken" to true,
                "manufacturer" to "HI-TEC™",
                "model" to "🍨",
                "orientation" to "sideup",
                "osName" to "BOX BOX",
                "osVersion" to "98.7",
                "runtimeVersions" to mapOf(
                    "osBuild" to "beta1-2",
                    "androidApiLevel" to "32"
                ),
                "time" to "2021-12-08T19:43:50Z",
                "totalMemory" to 3839512576L
            ),
            output["device"]
        )

        // feature flags
        assertEquals(
            listOf(
                mapOf(
                    "featureFlag" to "bluebutton",
                    "variant" to "on"
                ),
                mapOf(
                    "featureFlag" to "redbutton",
                    "variant" to "off"
                ),
                mapOf("featureFlag" to "nobutton"),
                mapOf(
                    "featureFlag" to "switch",
                    "variant" to "left"
                )
            ),
            output["featureFlags"]
        )

        // exceptions
        assertEquals(
            listOf(
                mapOf(
                    "errorClass" to "SIGBUS",
                    "message" to "POSIX is serious about oncoming traffic",
                    "type" to "c",
                    "stacktrace" to listOf(
                        mapOf(
                            "frameAddress" to "0xfffffffe",
                            "lineNumber" to 4194967233L,
                            "loadAddress" to "0x242023",
                            "symbolAddress" to "0x308",
                            "method" to "makinBacon",
                            "file" to "lib64/libfoo.so",
                            "isPC" to true
                        ),
                        mapOf(
                            "frameAddress" to "0xb37a644b",
                            "lineNumber" to 0L,
                            "loadAddress" to "0x0",
                            "symbolAddress" to "0x0",
                            "method" to "0xb37a644b" // test address to method hex
                        )
                    )
                )
            ),
            output["exceptions"]
        )

        // metadata
        assertEquals(
            mapOf(
                "app" to mapOf(
                    "activeScreen" to "Menu",
                    "weather" to "rain"
                ),
                "metrics" to mapOf(
                    "experimentX" to false,
                    "subject" to "percy",
                    "counter" to 47.5.toBigDecimal()
                )
            ),
            output["metaData"]
        )

        // session info
        assertEquals(
            mapOf(
                "id" to "aaaaaaaaaaaaaaaa",
                "startedAt" to "2031-07-09T11:08:21+00:00",
                "events" to mapOf(
                    "handled" to 5L,
                    "unhandled" to 2L
                )
            ),
            output["session"]
        )

        // threads
        val threads = output["threads"]
        if (threads is List<Any?>) {
            assertEquals(8, threads.size)
            threads.forEachIndexed { index, thread ->
                assertEquals(
                    mapOf(
                        "name" to "Thread #$index",
                        "state" to "paused-$index",
                        "id" to 1000L + index,
                        "type" to "c"
                    ),
                    thread
                )
            }
        } else {
            fail("threads is not a list of thread objects?!")
        }

        // user
        assertEquals(
            mapOf(
                "email" to "fenton@io.example.com",
                "name" to "Fenton",
                "id" to "fex01"
            ),
            output["user"]
        )
    }

    /** Migrate an event to the latest format, writing JSON to tempFilePath */
    external fun migrateEvent(tempFilePath: String)

    /** Migrate notifier and apiKey info to a bespoke structure (apiKey and
     * notifier are not included in event info written to disk) */
    external fun migratePayloadInfo(tempFilePath: String): String
}
