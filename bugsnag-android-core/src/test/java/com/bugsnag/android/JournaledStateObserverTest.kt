package com.bugsnag.android

import android.content.Context
import com.bugsnag.android.internal.ImmutableConfig
import org.junit.Assert
import org.junit.Before
import org.junit.ClassRule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.junit.MockitoJUnitRunner
import java.io.File

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

    private fun journalContaining(document: Map<String, Any>): BugsnagJournal {
        return BugsnagJournal(NoopLogger, baseDocumentPath, document).apply { snapshot() }
    }

    private fun emptyJournal(): BugsnagJournal {
        return journalContaining(mapOf<String, Any>())
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

        Mockito.`when`(client.getAppDataCollector()).thenReturn(appDataCollector)
        Mockito.`when`(client.getDeviceDataCollector()).thenReturn(deviceDataCollector)
        Mockito.`when`(client.getUser()).thenReturn(User("123", "tod@example.com", "Tod"))
    }

    @Test
    fun testInstall() {
        Mockito.`when`(appDataCollector.generateAppWithState())
            .thenReturn(BugsnagTestUtils.generateAppWithState())
        Mockito.`when`(appDataCollector.getAppDataMetadata())
            .thenReturn(mutableMapOf(Pair("metadata", true)))
        Mockito.`when`(deviceDataCollector.generateDeviceWithState(ArgumentMatchers.anyLong()))
            .thenReturn(BugsnagTestUtils.generateDeviceWithState())
        Mockito.`when`(deviceDataCollector.getDeviceMetadata())
            .thenReturn(mapOf(Pair("metadata", true)))

        val journal = emptyJournal()
        val observer = JournaledStateObserver(client, journal)
        observer.onStateChange(StateEvent.Install("myapikey", true, "myversion", "myuuid", "myrelease", "/somepath", 0))

        val doc = journal.document
        Assert.assertNotNull(doc["app"])
        Assert.assertEquals("myapikey", doc["apiKey"])
        Assert.assertNotNull(doc["device"])

        BugsnagTestUtils.assertNormalizedEquals(
            mapOf(
                "app" to mapOf(
                    "metadata" to true
                ),
                "device" to mapOf(
                    "metadata" to true
                )
            ),
            doc["metaData"]
        )

        BugsnagTestUtils.assertNormalizedEquals(
            mapOf(
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
            mapOf(
                "metaData" to mapOf(
                    "mysection" to mapOf(
                        "mykey" to "myvalue"
                    )
                )
            )
        )
        BugsnagTestUtils.assertNormalizedEquals(expected, journal.document)

        observer.onStateChange(StateEvent.ClearMetadataSection("mysection"))
        expected = BugsnagJournal.withInitialDocumentContents(
            mapOf(
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
                "mytime",
                mutableMapOf(
                    "x" to 1,
                    "y" to 2,
                    "z" to listOf(1, 2, 3)
                )
            )
        )
        val expected = BugsnagJournal.withInitialDocumentContents(
            mapOf(
                "breadcrumbs" to listOf(
                    mapOf(
                        "name" to "mymsg",
                        "timestamp" to "mytime",
                        "type" to "log",
                        "metaData" to mapOf(
                            "x" to 1,
                            "y" to 2,
                            "z" to listOf(1, 2, 3)
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
            mapOf(
                "session" to mapOf(
                    "events" to mapOf(
                        "handled" to 100
                    )
                )
            )
        )

        var expected = BugsnagJournal.withInitialDocumentContents(
            mapOf(
                "session" to mapOf(
                    "events" to mapOf(
                        "handled" to 100
                    )
                )
            )
        )
        BugsnagTestUtils.assertNormalizedEquals(expected, journal.document)

        val observer = JournaledStateObserver(client, journal)
        observer.onStateChange(StateEvent.NotifyHandled)
        expected = BugsnagJournal.withInitialDocumentContents(
            mapOf(
                "session" to mapOf(
                    "events" to mapOf(
                        "handled" to 101
                    )
                )
            )
        )
        BugsnagTestUtils.assertNormalizedEquals(expected, journal.document)

        observer.onStateChange(StateEvent.NotifyHandled)
        expected = BugsnagJournal.withInitialDocumentContents(
            mapOf(
                "session" to mapOf(
                    "events" to mapOf(
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
            mapOf(
                "session" to mapOf(
                    "events" to mapOf(
                        "unhandled" to 1
                    )
                )
            )
        )
        BugsnagTestUtils.assertNormalizedEquals(expected, journal.document)

        observer.onStateChange(StateEvent.NotifyUnhandled)
        expected = BugsnagJournal.withInitialDocumentContents(
            mapOf(
                "session" to mapOf(
                    "events" to mapOf(
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
            mapOf(
                "session" to mapOf(
                    "id" to "myid",
                    "startedAt" to "mytime",
                    "events" to mapOf(
                        "handled" to 10,
                        "unhandled" to 20
                    )
                )
            )
        )
        BugsnagTestUtils.assertNormalizedEquals(expected, journal.document)

        observer.onStateChange(StateEvent.PauseSession)
        expected = BugsnagJournal.withInitialDocumentContents(mapOf())
        BugsnagTestUtils.assertNormalizedEquals(expected, journal.document)
    }

    @Test
    fun testContext() {
        val journal = emptyJournal()
        val observer = JournaledStateObserver(client, journal)
        observer.onStateChange(StateEvent.UpdateContext("mycontext"))
        val expected = BugsnagJournal.withInitialDocumentContents(
            mapOf(
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
            mapOf(
                "app" to mapOf(
                    "inForeground" to true
                ),
                "metaData" to mapOf(
                    "app" to mapOf(
                        "activeScreen" to "myContextActivity"
                    )
                )
            )
        )
        BugsnagTestUtils.assertNormalizedEquals(expected, journal.document)
    }

    @Test
    fun testIsLaunching() {
        val journal = emptyJournal()
        val observer = JournaledStateObserver(client, journal)
        observer.onStateChange(StateEvent.UpdateIsLaunching(true))
        var expected = BugsnagJournal.withInitialDocumentContents(
            mapOf(
                "app" to mapOf(
                    "isLaunching" to true
                )
            )
        )
        BugsnagTestUtils.assertNormalizedEquals(expected, journal.document)

        observer.onStateChange(StateEvent.UpdateIsLaunching(false))
        expected = BugsnagJournal.withInitialDocumentContents(
            mapOf(
                "app" to mapOf(
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
            mapOf(
                "device" to mapOf(
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
            mapOf(
                "user" to mapOf(
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
            mapOf(
                "metaData" to mapOf(
                    "app" to mapOf(
                        "lowMemory" to true
                    )
                )
            )
        )
        BugsnagTestUtils.assertNormalizedEquals(expected, journal.document)
    }
}
