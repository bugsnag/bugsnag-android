package com.bugsnag.android

import android.content.Context
import com.bugsnag.android.internal.ImmutableConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.ClassRule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.anyLong
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.junit.MockitoJUnitRunner
import java.io.File
import java.util.Date

@RunWith(MockitoJUnitRunner::class)
internal class JournaledStateObserverTest {
    companion object {
        @ClassRule
        @JvmField
        val folder: TemporaryFolder = TemporaryFolder()
    }
    private val baseDocumentPath by lazy { File(folder.root, "mydocument") }

    fun generateAppWithState(): AppWithState {
        return AppWithState(
            BugsnagTestUtils.generateImmutableConfig(), null, null, null,
            null, null, null, null, null, null
        )
    }

    private fun journalContaining(document: MutableMap<String, Any>): BugsnagJournal {
        return BugsnagJournal(NoopLogger, baseDocumentPath, document).apply { snapshot() }
    }

    private fun emptyJournal(): BugsnagJournal {
        return journalContaining(mutableMapOf())
    }

    @Mock
    lateinit var client: Client

    @Mock
    lateinit var immutableConfig: ImmutableConfig

    @Mock
    lateinit var context: Context

    @Mock
    lateinit var appDataCollector: AppDataCollector

    @Mock
    lateinit var deviceDataCollector: DeviceDataCollector

    @Before
    fun setUp() {
        NativeInterface.setClient(client)

        `when`(client.getAppDataCollector()).thenReturn(appDataCollector)
        `when`(client.getDeviceDataCollector()).thenReturn(deviceDataCollector)
        `when`(client.getUser()).thenReturn(User("123", "tod@example.com", "Tod"))
        `when`(client.config).thenReturn(immutableConfig)
        `when`(immutableConfig.projectPackages).thenReturn(mutableListOf("com.example.foo"))
    }

    @Test
    fun testInstall() {
        `when`(appDataCollector.generateAppWithState())
            .thenReturn(BugsnagTestUtils.generateAppWithState())
        `when`(appDataCollector.getAppDataMetadata())
            .thenReturn(mutableMapOf(Pair("metadata", true)))
        `when`(deviceDataCollector.generateDeviceWithState(anyLong()))
            .thenReturn(BugsnagTestUtils.generateDeviceWithState())
        `when`(deviceDataCollector.getDeviceMetadata())
            .thenReturn(mutableMapOf(Pair("metadata", true)))

        val journal = emptyJournal()
        val observer = JournaledStateObserver(client, journal)
        observer.onStateChange(StateEvent.JournalSetup("myapikey"))

        val doc = journal.document
        assertNotNull(doc["app"])
        assertEquals("myapikey", doc["apiKey"])
        assertEquals(mutableListOf("com.example.foo"), doc["projectPackages"])
        assertNotNull(doc["device"])

        BugsnagTestUtils.assertNormalizedEquals(
            mutableMapOf(
                "app" to mutableMapOf(
                    "metadata" to true
                ),
                "device" to mutableMapOf(
                    "metadata" to true
                )
            ),
            doc["metaData"]
        )

        BugsnagTestUtils.assertNormalizedEquals(
            mutableMapOf(
                "name" to "Tod",
                "email" to "tod@example.com",
                "id" to "123"
            ),
            doc["user"]
        )
    }

    @Test
    fun testMetadata() {
        val journal = emptyJournal()
        val observer = JournaledStateObserver(client, journal)
        observer.onStateChange(StateEvent.AddMetadata("mysection", "mykey", "myvalue"))
        var expected = BugsnagJournal.withInitialDocumentContents(
            mutableMapOf(
                "metaData" to mutableMapOf(
                    "mysection" to mutableMapOf(
                        "mykey" to "myvalue"
                    )
                )
            )
        )
        BugsnagTestUtils.assertNormalizedEquals(expected, journal.document)

        observer.onStateChange(StateEvent.ClearMetadataSection("mysection"))
        expected = BugsnagJournal.withInitialDocumentContents(
            mutableMapOf(
                "metaData" to mapOf<String, Any>()
            )
        )
        BugsnagTestUtils.assertNormalizedEquals(expected, journal.document)
    }

    @Test
    fun testMetadataNumeric() {
        val journal = emptyJournal()
        val observer = JournaledStateObserver(client, journal)
        observer.onStateChange(StateEvent.AddMetadata("0", "-1", "myvalue"))
        var expected = BugsnagJournal.withInitialDocumentContents(
            mutableMapOf(
                "metaData" to mutableMapOf(
                    "0" to mutableMapOf(
                        "-1" to "myvalue"
                    )
                )
            )
        )
        BugsnagTestUtils.assertNormalizedEquals(expected, journal.document)

        observer.onStateChange(StateEvent.ClearMetadataSection("0"))
        expected = BugsnagJournal.withInitialDocumentContents(
            mutableMapOf(
                "metaData" to mapOf<String, Any>()
            )
        )
        BugsnagTestUtils.assertNormalizedEquals(expected, journal.document)
    }

    @Test
    fun testMetadataSpecialChars() {
        val journal = emptyJournal()
        val observer = JournaledStateObserver(client, journal)
        observer.onStateChange(StateEvent.AddMetadata("0\\1.4", "a+", "myvalue"))
        var expected = BugsnagJournal.withInitialDocumentContents(
            mutableMapOf(
                "metaData" to mutableMapOf(
                    "0\\1.4" to mutableMapOf(
                        "a+" to "myvalue"
                    )
                )
            )
        )
        BugsnagTestUtils.assertNormalizedEquals(expected, journal.document)

        observer.onStateChange(StateEvent.ClearMetadataSection("0\\1.4"))
        expected = BugsnagJournal.withInitialDocumentContents(
            mutableMapOf(
                "metaData" to mapOf<String, Any>()
            )
        )
        BugsnagTestUtils.assertNormalizedEquals(expected, journal.document)
    }

    @Test
    fun testBreadcrumbs() {
        val journal = emptyJournal()
        val observer = JournaledStateObserver(client, journal)
        observer.onStateChange(
            StateEvent.AddBreadcrumb(
                "mymsg",
                BreadcrumbType.LOG,
                Date(1636127079133),
                mutableMapOf(
                    "x" to 1,
                    "y" to 2,
                    "z" to mutableListOf(1, 2, 3)
                )
            )
        )
        val expected = BugsnagJournal.withInitialDocumentContents(
            mutableMapOf(
                "breadcrumbs" to mutableListOf(
                    mutableMapOf(
                        "name" to "mymsg",
                        "timestamp" to 1636127079133L,
                        "type" to "log",
                        "metaData" to mutableMapOf(
                            "x" to 1,
                            "y" to 2,
                            "z" to mutableListOf(1, 2, 3)
                        )
                    )
                )
            )
        )
        BugsnagTestUtils.assertNormalizedEquals(expected, journal.document)
    }

    @Test
    fun testHandled() {
        val journal = journalContaining(
            mutableMapOf(
                "session" to mutableMapOf(
                    "events" to mutableMapOf(
                        "handled" to 100
                    )
                )
            )
        )

        var expected = BugsnagJournal.withInitialDocumentContents(
            mutableMapOf(
                "session" to mutableMapOf(
                    "events" to mutableMapOf(
                        "handled" to 100
                    )
                )
            )
        )
        BugsnagTestUtils.assertNormalizedEquals(expected, journal.document)

        val observer = JournaledStateObserver(client, journal)
        observer.onStateChange(StateEvent.NotifyHandled)
        expected = BugsnagJournal.withInitialDocumentContents(
            mutableMapOf(
                "session" to mutableMapOf(
                    "events" to mutableMapOf(
                        "handled" to 101
                    )
                )
            )
        )
        BugsnagTestUtils.assertNormalizedEquals(expected, journal.document)

        observer.onStateChange(StateEvent.NotifyHandled)
        expected = BugsnagJournal.withInitialDocumentContents(
            mutableMapOf(
                "session" to mutableMapOf(
                    "events" to mutableMapOf(
                        "handled" to 102
                    )
                )
            )
        )
        BugsnagTestUtils.assertNormalizedEquals(expected, journal.document)
    }

    @Test
    fun testUnhandled() {
        val journal = emptyJournal()
        val observer = JournaledStateObserver(client, journal)
        observer.onStateChange(StateEvent.NotifyUnhandled)
        var expected = BugsnagJournal.withInitialDocumentContents(
            mutableMapOf(
                "session" to mutableMapOf(
                    "events" to mutableMapOf(
                        "unhandled" to 1
                    )
                )
            )
        )
        BugsnagTestUtils.assertNormalizedEquals(expected, journal.document)

        observer.onStateChange(StateEvent.NotifyUnhandled)
        expected = BugsnagJournal.withInitialDocumentContents(
            mutableMapOf(
                "session" to mutableMapOf(
                    "events" to mutableMapOf(
                        "unhandled" to 2
                    )
                )
            )
        )
        BugsnagTestUtils.assertNormalizedEquals(expected, journal.document)
    }

    @Test
    fun testSession() {
        val journal = emptyJournal()
        val observer = JournaledStateObserver(client, journal)
        observer.onStateChange(StateEvent.StartSession("myid", "mytime", 10, 20))
        var expected = BugsnagJournal.withInitialDocumentContents(
            mutableMapOf(
                "session" to mutableMapOf(
                    "id" to "myid",
                    "startedAt" to "mytime",
                    "events" to mutableMapOf(
                        "handled" to 10,
                        "unhandled" to 20
                    )
                )
            )
        )
        BugsnagTestUtils.assertNormalizedEquals(expected, journal.document)

        observer.onStateChange(StateEvent.PauseSession)
        expected = BugsnagJournal.withInitialDocumentContents(mutableMapOf())
        BugsnagTestUtils.assertNormalizedEquals(expected, journal.document)
    }

    @Test
    fun testContext() {
        val journal = emptyJournal()
        val observer = JournaledStateObserver(client, journal)
        observer.onStateChange(StateEvent.UpdateContext("mycontext"))
        val expected = BugsnagJournal.withInitialDocumentContents(
            mutableMapOf(
                "context" to "mycontext"
            )
        )
        BugsnagTestUtils.assertNormalizedEquals(expected, journal.document)
    }

    @Test
    fun testInForeground() {
        val journal = emptyJournal()
        val observer = JournaledStateObserver(client, journal)
        observer.onStateChange(StateEvent.UpdateInForeground(true, "myContextActivity"))
        val expected = BugsnagJournal.withInitialDocumentContents(
            mutableMapOf(
                "app" to mutableMapOf(
                    "inForeground" to true
                ),
                "metaData" to mutableMapOf(
                    "app" to mutableMapOf(
                        "activeScreen" to "myContextActivity"
                    )
                )
            )
        )
        val actual = journal.document.filterKeys { it == "version-info" || it == "app" || it == "metaData" }
        BugsnagTestUtils.assertNormalizedEquals(expected, actual)
    }

    @Test
    fun testIsLaunching() {
        val journal = emptyJournal()
        val observer = JournaledStateObserver(client, journal)
        observer.onStateChange(StateEvent.UpdateIsLaunching(true))
        var expected = BugsnagJournal.withInitialDocumentContents(
            mutableMapOf(
                "app" to mutableMapOf(
                    "isLaunching" to true
                )
            )
        )
        BugsnagTestUtils.assertNormalizedEquals(expected, journal.document)

        observer.onStateChange(StateEvent.UpdateIsLaunching(false))
        expected = BugsnagJournal.withInitialDocumentContents(
            mutableMapOf(
                "app" to mutableMapOf(
                    "isLaunching" to false
                )
            )
        )
        BugsnagTestUtils.assertNormalizedEquals(expected, journal.document)
    }

    @Test
    fun testOrientation() {
        val journal = emptyJournal()
        val observer = JournaledStateObserver(client, journal)
        observer.onStateChange(StateEvent.UpdateOrientation("myorientation"))
        val expected = BugsnagJournal.withInitialDocumentContents(
            mutableMapOf(
                "device" to mutableMapOf(
                    "orientation" to "myorientation"
                )
            )
        )
        BugsnagTestUtils.assertNormalizedEquals(expected, journal.document)
    }

    @Test
    fun testUpdateUser() {
        val journal = emptyJournal()
        val observer = JournaledStateObserver(client, journal)
        observer.onStateChange(StateEvent.UpdateUser(User("myid", "myemail@x.com", "myname")))
        val expected = BugsnagJournal.withInitialDocumentContents(
            mutableMapOf(
                "user" to mutableMapOf(
                    "id" to "myid",
                    "email" to "myemail@x.com",
                    "name" to "myname"
                )
            )
        )
        BugsnagTestUtils.assertNormalizedEquals(expected, journal.document)
    }

    @Test
    fun testMemoryTrim() {
        val journal = emptyJournal()
        val observer = JournaledStateObserver(client, journal)
        observer.onStateChange(StateEvent.UpdateMemoryTrimEvent(true))
        val expected = BugsnagJournal.withInitialDocumentContents(
            mutableMapOf(
                "metaData" to mutableMapOf(
                    "app" to mutableMapOf(
                        "lowMemory" to true,
                        "memoryTrimLevel" to "None"
                    )
                )
            )
        )
        BugsnagTestUtils.assertNormalizedEquals(expected, journal.document)
    }

    @Test
    fun testNotifierInfo() {
        val journal = emptyJournal()
        val observer = JournaledStateObserver(client, journal)
        observer.onStateChange(StateEvent.UpdateNotifierInfo(Notifier(version = "1.2.3")))
        val expected = BugsnagJournal.withInitialDocumentContents(
            mutableMapOf(
                "notifier" to mutableMapOf(
                    "name" to "Android Bugsnag Notifier",
                    "version" to "1.2.3",
                    "url" to "https://bugsnag.com"
                )
            )
        )
        BugsnagTestUtils.assertNormalizedEquals(expected, journal.document)
    }
}
