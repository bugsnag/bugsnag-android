package com.bugsnag.android

import android.app.Application
import android.content.Context
import androidx.annotation.VisibleForTesting
import com.bugsnag.android.Metadata.Companion.merge
import com.bugsnag.android.SeverityReason.SeverityReasonType
import com.bugsnag.android.SystemBroadcastReceiver.Companion.register
import com.bugsnag.android.internal.DateUtils
import com.bugsnag.android.internal.ImmutableConfig
import com.bugsnag.android.internal.StateObserver
import com.bugsnag.android.internal.dag.ConfigModule
import com.bugsnag.android.internal.dag.ContextModule
import com.bugsnag.android.internal.dag.SystemServiceModule
import com.bugsnag.android.internal.journal.BugsnagJournalStore
import com.bugsnag.android.internal.journal.JournalKeys
import com.bugsnag.android.internal.journal.JournaledDocument
import java.io.File
import java.lang.IllegalArgumentException
import java.util.Date
import java.util.concurrent.Callable
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.atomic.AtomicBoolean

internal class ClientInternal constructor(
    androidContext: Context,
    configuration: Configuration,
    private val client: Client
) : CallbackAware, MetadataAware, UserAware {

    // core client properties
    val appContext: Context
    val bgTaskService: BackgroundTaskService
    val config: ImmutableConfig
    private val journal: Lazy<BugsnagJournal>
    private val journalStore: BugsnagJournalStore

    // logger
    val logger: Logger

    // mutable state tracked by Bugsnag
    val breadcrumbState: BreadcrumbState
    val contextState: ContextState
    private val callbackState: CallbackState
    val memoryTrimState: MemoryTrimState
    val metadataState: MetadataState
    val notifierState: NotifierState
    private val userState: UserState
    private val crashedLastLaunch = AtomicBoolean(false)
    private val initialized = AtomicBoolean(false)

    // data collection
    val appDataCollector: AppDataCollector
    val deviceDataCollector: DeviceDataCollector
    private val exceptionHandler: ExceptionHandler
    private val systemBroadcastReceiver: SystemBroadcastReceiver

    // persistence
    val eventStore: EventStore
    val lastRunInfo: LastRunInfo?
    private val lastRunInfoStore: LastRunInfoStore
    val launchCrashTracker: LaunchCrashTracker
    val sessionTracker: SessionTracker

    // error delivery
    private val connectivity: Connectivity
    private val deliveryDelegate: DeliveryDelegate

    // observers + plugins
    private val clientObservable: ClientObservable
    private val pluginClient: PluginClient
    private val observables: List<BaseObservable>

    /**
     * Constructs all the objects required for error reporting. Performing operations
     * in this constructor should be avoided unless absolutely necessary.
     */
    init {
        val launchTime = Date()
        bgTaskService = BackgroundTaskService()
        memoryTrimState = MemoryTrimState()
        val contextModule = ContextModule(androidContext)
        appContext = contextModule.ctx
        connectivity = ConnectivityCompat(appContext, this::onConnectivityChange)

        // set sensible defaults for delivery/project packages etc if not set
        val configModule = ConfigModule(contextModule, configuration, connectivity)
        config = configModule.config
        logger = config.logger
        warnIfNotAppContext(androidContext)

        // setup journal
        journalStore = BugsnagJournalStore(config.journalBasePath, logger)
        journal = lazy {
            journalStore.createNewJournal().apply {
                addCommand(JournalKeys.pathRuntimeLaunchTime, DateUtils.toIso8601(launchTime))
            }
        }

        // setup storage as soon as possible
        val storageModule = StorageModule(
            appContext,
            config, logger
        )

        // setup state trackers for bugsnag
        val bugsnagStateModule = BugsnagStateModule(configModule, configuration)
        clientObservable = bugsnagStateModule.clientObservable
        callbackState = bugsnagStateModule.callbackState
        breadcrumbState = bugsnagStateModule.breadcrumbState
        contextState = bugsnagStateModule.contextState
        metadataState = bugsnagStateModule.metadataState
        notifierState = bugsnagStateModule.notifierState

        // lookup system services
        val systemServiceModule = SystemServiceModule(contextModule)

        // block until storage module has resolved everything
        storageModule.resolveDependencies(bgTaskService, TaskType.IO)

        // setup further state trackers and data collection
        val trackerModule = TrackerModule(
            contextModule,
            configModule,
            storageModule, client, bgTaskService, callbackState
        )
        launchCrashTracker = trackerModule.launchCrashTracker
        sessionTracker = trackerModule.sessionTracker
        val dataCollectionModule = DataCollectionModule(
            contextModule,
            configModule, systemServiceModule, trackerModule,
            bgTaskService, connectivity, storageModule.deviceId, memoryTrimState
        )
        dataCollectionModule.resolveDependencies(bgTaskService, TaskType.IO)
        appDataCollector = dataCollectionModule.appDataCollector
        deviceDataCollector = dataCollectionModule.deviceDataCollector

        // load the device + user information
        userState = storageModule.userStore.load(configuration.getUser())
        storageModule.sharedPrefMigrator.deleteLegacyPrefs()

        val notifier = notifierState.notifier
        val eventStorageModule = EventStorageModule(
            contextModule, configModule,
            dataCollectionModule, bgTaskService, trackerModule, systemServiceModule,
            notifier, callbackState
        )
        eventStorageModule.resolveDependencies(bgTaskService, TaskType.IO)
        eventStore = eventStorageModule.eventStore
        deliveryDelegate = DeliveryDelegate(
            logger, eventStore,
            config, breadcrumbState, notifier, bgTaskService
        )

        // Install a default exception handler with this client
        exceptionHandler = ExceptionHandler(client, logger)

        // load last run info
        lastRunInfoStore = storageModule.lastRunInfoStore
        lastRunInfo = storageModule.lastRunInfo

        // initialise plugins before attempting to flush any errors
        val userPlugins = configuration.plugins
        pluginClient = PluginClient(userPlugins, config, logger)
        systemBroadcastReceiver = SystemBroadcastReceiver(client, config, logger)

        // add observer before syncing initial state
        observables = listOf(
            metadataState,
            breadcrumbState,
            sessionTracker,
            clientObservable,
            userState,
            contextState,
            deliveryDelegate,
            launchCrashTracker,
            memoryTrimState,
            notifierState
        )
    }

    /**
     * Initializes Bugsnag. This is achieved in several steps to ensure that errors can be
     * captured as early as possible:
     *
     * 1. Create an empty BugsnagJournal
     * 2. Install exception, signal, and ANR handlers
     * 3. Populate the journal with initial entries
     * 4. Start collection of automatic data (such as lifecycle breadcrumbs)
     * 5. Flush any cached payloads
     * 6. Log a breadcrumb that Bugsnag finished loading
     */
    fun start() {
        createBugsnagJournal()
        installErrorHandlers()
        addInitialJournalEntries()
        syncInitialState()
        installDataCollectors()
        flushCachedPayloads()
        leaveBugsnagLoadedCrumb()
    }

    /**
     * Creates an empty Bugsnag Journal which can be used for recording errors and system state.
     */
    private fun createBugsnagJournal() {
        if (isJournalDisabled()) {
            return
        }
        journal.value
    }

    /**
     * Installs any exception/ANR/signal handlers.
     */
    private fun installErrorHandlers() {
        if (config.shouldDiscardByReleaseStage()) {
            return
        }
        // this will be used to determine whether events from the journal should be sent
        // as a synchronous event
        crashedLastLaunch.set(launchCrashTracker.crashedDuringLastLaunch())
        launchCrashTracker.startAutoTracking(config)

        if (config.enabledErrorTypes.unhandledExceptions) {
            exceptionHandler.install()
        }

        NativeInterface.setClient(client)
        pluginClient.loadNdkPlugin(client)
        pluginClient.loadAnrPlugin(client)
        pluginClient.loadReactNativePlugin(client)
    }

    /**
     * Populates the Bugsnag Journal with some initial state.
     */
    private fun addInitialJournalEntries() {
        if (isJournalDisabled()) {
            return
        }
        val observer = JournaledStateObserver(client, journal.value)
        observer.onStateChange(StateEvent.JournalSetup(config.apiKey))
        addObserver(observer)
    }

    /**
     * Installs callbacks/broadcast receivers which are used to collect automatic data.
     */
    private fun installDataCollectors() {
        registerLifecycleCallbacks()
        registerComponentCallbacks()
        registerListenersInBackground()
        pluginClient.loadUserPlugins(client)
    }

    /**
     * Flushes any payloads which are cached on disk.
     */
    private fun flushCachedPayloads() {
        // process launch crashes immediately
        processJournalLaunchCrash()

        // this flushes any launch crashes immediately
        eventStore.flushOnLaunch()

        // all other requests are sent in the background
        eventStore.flushAsync()
        processJournalsInBg()
        sessionTracker.flushAsync()
    }

    private fun processJournalLaunchCrash() {
        if (isJournalDisabled()) {
            return
        }
        if (crashedLastLaunch.get()) {
            journalStore.processMostRecentJournal { event ->
                sanitizeJournalEvent(event)
                eventStore.write(event, crashedLastLaunch.get())
            }
        }
    }

    private fun processJournalsInBg() {
        if (isJournalDisabled()) {
            return
        }
        try {
            bgTaskService.submitTask(
                TaskType.IO,
                Runnable {
                    journalStore.processPreviousJournals { event ->
                        sanitizeJournalEvent(event)
                        eventStore.write(event, crashedLastLaunch.get())
                    }
                    eventStore.flushAsync()
                }
            )
        } catch (exc: RejectedExecutionException) {
            logger.w("Failed to process journal files", exc)
        }
    }

    /**
     * Leaves a breadcrumb that Bugsnag has loaded.
     */
    private fun leaveBugsnagLoadedCrumb() {
        initialized.set(true)
        leaveAutoBreadcrumb("Bugsnag loaded", BreadcrumbType.STATE, emptyMap())
        logger.d("Bugsnag loaded")
    }

    private fun sanitizeJournalEvent(event: EventInternal) {
        // remove some fields which are in the journal but not relevant to NDK events
        event.device.freeDisk = null
        event.device.freeMemory = null
        event.metadata.clearMetadata("app", "freeMemory")
        event.metadata.clearMetadata("app", "memoryUsage")
        event.metadata.clearMetadata("app", "networkAccess")
        event.metadata.clearMetadata("app", "totalMemory")
        event.metadata.clearMetadata("device", "batteryLevel")
        event.metadata.clearMetadata("device", "charging")
        event.metadata.clearMetadata("device", "freeMemory")
        event.metadata.clearMetadata("device", "totalMemory")
    }

    private fun onConnectivityChange(hasConnection: Boolean, networkState: String?) {
        leaveAutoBreadcrumb(
            "Connectivity changed", BreadcrumbType.STATE,
            mapOf(
                "hasConnection" to hasConnection,
                "networkState" to networkState
            )
        )

        // if not initialized or connected then we should not attempt to flush payloads
        if (hasConnection && initialized.get()) {
            eventStore.flushAsync()
            sessionTracker.flushAsync()
        }

        if (isJournalDisabled()) {
            return
        }
        journal.value.addCommand(JournalKeys.pathMetadataAppNetworkAccess, networkState)
    }

    /**
     * Checks whether the journal is enabled or not. Disabling the journal improves performance
     * when NDK errors are not being recorded.
     */
    private fun isJournalDisabled() = !config.enabledErrorTypes.ndkCrashes

    private fun registerLifecycleCallbacks() {
        if (appContext is Application) {
            val sessionCb = SessionLifecycleCallback(sessionTracker)
            appContext.registerActivityLifecycleCallbacks(sessionCb)

            if (!config.shouldDiscardBreadcrumb(BreadcrumbType.STATE)) {
                val activityCb =
                    ActivityBreadcrumbCollector { activity: String, metadata: Map<String, Any> ->
                        leaveBreadcrumb(activity, metadata, BreadcrumbType.STATE)
                    }
                appContext.registerActivityLifecycleCallbacks(activityCb)
            }
        }
    }

    private fun registerListenersInBackground() {
        try {
            val runnable = Runnable {
                connectivity.registerForNetworkChanges()
                register(appContext, systemBroadcastReceiver, logger)
            }
            bgTaskService.submitTask(TaskType.DEFAULT, runnable)
        } catch (ex: RejectedExecutionException) {
            logger.w("Failed to register for system events", ex)
        }
    }

    private fun persistRunInfo(runInfo: LastRunInfo) {
        try {
            bgTaskService.submitTask(
                TaskType.IO,
                Runnable {
                    lastRunInfoStore.persist(runInfo)
                }
            )
        } catch (exc: RejectedExecutionException) {
            logger.w("Failed to persist last run info", exc)
        }
    }

    private fun registerComponentCallbacks() {
        appContext.registerComponentCallbacks(
            ClientComponentCallbacks(
                deviceDataCollector,
                { oldOrientation: String?, newOrientation: String? ->
                    leaveAutoBreadcrumb(
                        "Orientation changed", BreadcrumbType.STATE,
                        mapOf(
                            "from" to oldOrientation,
                            "to" to newOrientation
                        )
                    )
                    clientObservable.postOrientationChange(newOrientation)
                }
            ) { isLowMemory: Boolean, memoryTrimLevel: Int? ->
                memoryTrimState.isLowMemory = (java.lang.Boolean.TRUE == isLowMemory)
                if (memoryTrimState.updateMemoryTrimLevel(memoryTrimLevel)) {
                    leaveAutoBreadcrumb(
                        "Trim Memory",
                        BreadcrumbType.STATE,
                        mapOf("trimLevel" to memoryTrimState.trimLevelDescription)
                    )
                }
                memoryTrimState.emitObservableEvent()
            }
        )
    }

    fun setupNdkPlugin() {
        if (!setupNdkDirectory()) {
            logger.w("Failed to setup NDK directory.")
            return
        }
        val lastRunInfoPath = lastRunInfoStore.file.absolutePath
        val crashes = lastRunInfo?.consecutiveLaunchCrashes ?: 0
        clientObservable.postNdkInstall(config, lastRunInfoPath, crashes)
        clientObservable.postNdkDeliverPending()
    }

    private fun setupNdkDirectory(): Boolean {
        return try {
            val callable = Callable {
                val outFile = File(NativeInterface.getNativeReportPath())
                outFile.exists() || outFile.mkdirs()
            }
            bgTaskService.submitTask(TaskType.IO, callable).get()
        } catch (exc: Throwable) {
            false
        }
    }

    fun addObserver(observer: StateObserver) = observables.forEach {
        it.addObserver(observer)
    }

    fun removeObserver(observer: StateObserver) = observables.forEach {
        it.removeObserver(observer)
    }

    fun syncInitialState() = observables.forEach(BaseObservable::emitObservableEvent)

    // session tracking
    fun startSession(): Session = sessionTracker.startSession(false)
    fun pauseSession() = sessionTracker.pauseSession()
    fun resumeSession() = sessionTracker.resumeSession()

    // context tracking
    fun getContext() = contextState.getContext()
    fun setContext(context: String?) = contextState.setManualContext(context)

    // user tracking
    override fun setUser(id: String?, email: String?, name: String?) {
        userState.user = User(id, email, name)
    }

    override fun getUser(): User = userState.user

    // callbacks
    override fun addOnError(onError: OnErrorCallback) = callbackState.addOnError(onError)
    override fun removeOnError(onError: OnErrorCallback) = callbackState.removeOnError(onError)
    override fun addOnBreadcrumb(onBreadcrumb: OnBreadcrumbCallback) =
        callbackState.addOnBreadcrumb(onBreadcrumb)

    override fun removeOnBreadcrumb(onBreadcrumb: OnBreadcrumbCallback) =
        callbackState.removeOnBreadcrumb(onBreadcrumb)

    override fun addOnSession(onSession: OnSessionCallback) = callbackState.addOnSession(onSession)
    override fun removeOnSession(onSession: OnSessionCallback) =
        callbackState.removeOnSession(onSession)

    fun notify(exception: Throwable) = notify(exception, null)

    fun notify(exc: Throwable, onError: OnErrorCallback?) {
        if (config.shouldDiscardError(exc)) {
            return
        }
        val severityReason = SeverityReason.newInstance(SeverityReason.REASON_HANDLED_EXCEPTION)
        val metadata = metadataState.metadata
        val event = Event(exc, config, severityReason, metadata, logger)
        populateAndNotifyAndroidEvent(event, onError)
    }

    fun notifyUnhandledException(
        exc: Throwable,
        metadata: Metadata,
        @SeverityReasonType severityReason: String,
        attributeValue: String?
    ) {
        val handledState =
            SeverityReason.newInstance(severityReason, Severity.ERROR, attributeValue)
        val data = merge(metadataState.metadata, metadata)
        val event = Event(exc, config, handledState, data, logger)
        populateAndNotifyAndroidEvent(event, null)

        // persist LastRunInfo so that on relaunch users can check the app crashed
        var consecutiveLaunchCrashes = lastRunInfo?.consecutiveLaunchCrashes ?: 0
        val launching = launchCrashTracker.isLaunching()
        if (launching) {
            consecutiveLaunchCrashes += 1
        }
        val runInfo = LastRunInfo(consecutiveLaunchCrashes, true, launching)
        persistRunInfo(runInfo)

        // suspend execution of any further background tasks, waiting for previously
        // submitted ones to complete.
        bgTaskService.shutdown()
    }

    fun populateAndNotifyAndroidEvent(event: Event, onError: OnErrorCallback?) {
        // Capture the state of the app and device and attach diagnostics to the event
        event.device = deviceDataCollector.generateDeviceWithState(Date().time)
        event.addMetadata("device", deviceDataCollector.getDeviceMetadata())

        // add additional info that belongs in metadata
        // generate new object each time, as this can be mutated by end-users
        event.app = appDataCollector.generateAppWithState()
        event.addMetadata("app", appDataCollector.getAppDataMetadata())

        // Attach breadcrumbState to the event
        event.breadcrumbs = breadcrumbState.copy()

        // Attach user info to the event
        val user = userState.user
        event.setUser(user.id, user.email, user.name)

        // Attach context to the event
        event.context = contextState.getContext()
        notifyInternal(event, onError)
    }

    fun notifyInternal(event: Event, onError: OnErrorCallback?) {
        // set the redacted keys on the event as this
        // will not have been set for RN/Unity events
        val redactedKeys = metadataState.metadata.redactedKeys
        val eventMetadata = event.impl.metadata
        eventMetadata.redactedKeys = redactedKeys

        // get session for event
        val currentSession = sessionTracker.currentSession
        if (currentSession != null &&
            (config.autoTrackSessions || !currentSession.isAutoCaptured)
        ) {
            event.session = currentSession
        }

        // Run on error tasks, don't notify if any return false
        if (!callbackState.runOnErrorTasks(event, logger) ||
            onError != null && !onError.onError(event)
        ) {
            logger.d("Skipping notification - onError task returned false")
            return
        }
        deliveryDelegate.deliver(event)
    }

    fun getBreadcrumbs(): List<Breadcrumb?> = breadcrumbState.copy()

    // metadata tracking
    override fun addMetadata(section: String, value: Map<String, Any?>) =
        metadataState.addMetadata(section, value)

    override fun addMetadata(section: String, key: String, value: Any?) =
        metadataState.addMetadata(section, key, value)

    override fun clearMetadata(section: String) = metadataState.clearMetadata(section)
    override fun clearMetadata(section: String, key: String) =
        metadataState.clearMetadata(section, key)

    override fun getMetadata(section: String): Map<String, Any>? =
        metadataState.getMetadata(section)

    override fun getMetadata(section: String, key: String): Any? =
        metadataState.getMetadata(section, key)

    fun leaveBreadcrumb(message: String) {
        breadcrumbState.add(Breadcrumb(message, logger))
    }

    fun leaveBreadcrumb(message: String, metadata: Map<String, Any?>, type: BreadcrumbType) {
        breadcrumbState.add(Breadcrumb(message, type, metadata, Date(), logger))
    }

    fun leaveAutoBreadcrumb(message: String, type: BreadcrumbType, metadata: Map<String, Any?>) {
        if (!config.shouldDiscardBreadcrumb(type)) {
            breadcrumbState.add(Breadcrumb(message, type, metadata, Date(), logger))
        }
    }

    fun markLaunchCompleted() = launchCrashTracker.markLaunchCompleted()

    private fun warnIfNotAppContext(androidContext: Context) {
        if (androidContext !is Application) {
            logger.w(
                "Warning - Non-Application context detected! Please ensure that you are " +
                    "initializing Bugsnag from a custom Application class."
            )
        }
    }

    fun setBinaryArch(binaryArch: String) = appDataCollector.setBinaryArch(binaryArch)

    /**
     * Intended for internal use only - sets the code bundle id for React Native
     */
    fun getCodeBundleId() = appDataCollector.codeBundleId

    /**
     * Intended for internal use only - sets the code bundle id for React Native
     */
    fun setCodeBundleId(codeBundleId: String?) {
        appDataCollector.codeBundleId = codeBundleId
    }

    fun addRuntimeVersionInfo(key: String, value: String) =
        deviceDataCollector.addRuntimeVersionInfo(key, value)

    val crashtimeJournalPath =
        JournaledDocument.getCrashtimeJournalPath(journalStore.currentBasePath)

    @VisibleForTesting
    fun close() {
        connectivity.unregisterForNetworkChanges()
        bgTaskService.shutdown()

        try {
            appContext.unregisterReceiverSafe(systemBroadcastReceiver, logger)
        } catch (exception: IllegalArgumentException) {
            logger.w("Receiver not registered")
        }
    }

    fun getPlugin(clz: Class<*>) = pluginClient.findPlugin(clz)

    fun setNotifier(notifier: Notifier) {
        notifierState.notifier = notifier
    }

    fun getMetadata() = metadataState.metadata.toMap()

    fun setAutoNotify(autoNotify: Boolean) {
        pluginClient.setAutoNotify(client, autoNotify)
        if (autoNotify) {
            exceptionHandler.install()
        } else {
            exceptionHandler.uninstall()
        }
    }

    fun setAutoDetectAnrs(autoDetectAnrs: Boolean) {
        pluginClient.setAutoDetectAnrs(client, autoDetectAnrs)
    }

    fun addOnSend(onSend: OnSendCallback) {
        callbackState.addOnSend(onSend)
    }

    fun removeOnSend(onSend: OnSendCallback) {
        callbackState.removeOnSend(onSend)
    }
}
