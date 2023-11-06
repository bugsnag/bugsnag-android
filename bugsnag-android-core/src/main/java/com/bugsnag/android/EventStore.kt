@file:Suppress("NAME_SHADOWING")

package com.bugsnag.android

import com.bugsnag.android.EventFilenameInfo.Companion.findTimestampInFilename
import com.bugsnag.android.EventFilenameInfo.Companion.fromEvent
import com.bugsnag.android.EventFilenameInfo.Companion.fromFile
import com.bugsnag.android.JsonStream.Streamable
import com.bugsnag.android.internal.BackgroundTaskService
import com.bugsnag.android.internal.ImmutableConfig
import com.bugsnag.android.internal.TaskType
import java.io.File
import java.util.Calendar
import java.util.Comparator
import java.util.Date
import java.util.concurrent.Callable
import java.util.concurrent.ExecutionException
import java.util.concurrent.Future
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import kotlin.collections.ArrayList

/**
 * Store and flush Event reports which couldn't be sent immediately due to
 * lack of network connectivity.
 */
internal class EventStore(
    private val config: ImmutableConfig,
    logger: Logger,
    notifier: Notifier,
    bgTaskSevice: BackgroundTaskService,
    delegate: Delegate?,
    callbackState: CallbackState
) : FileStore(
    File(config.persistenceDirectory.value, "bugsnag/errors"),
    config.maxPersistedEvents,
    EVENT_COMPARATOR,
    logger,
    delegate
) {
    private val delegate: Delegate?
    private val notifier: Notifier
    private val bgTaskSevice: BackgroundTaskService
    private val callbackState: CallbackState
    override val logger: Logger

    /**
     * Flush startup crashes synchronously on the main thread
     */
    fun flushOnLaunch() {
        if (!config.sendLaunchCrashesSynchronously) {
            return
        }
        var future: Future<*>? = null
        try {
            future = bgTaskSevice.submitTask(
                TaskType.ERROR_REQUEST,
                Runnable { flushLaunchCrashReport() }
            )
        } catch (exc: RejectedExecutionException) {
            logger.d("Failed to flush launch crash reports, continuing.", exc)
        }
        try {
            future?.get(LAUNCH_CRASH_TIMEOUT_MS, TimeUnit.MILLISECONDS)
        } catch (exc: InterruptedException) {
            logger.d("Failed to send launch crash reports within 2s timeout, continuing.", exc)
        } catch (exc: ExecutionException) {
            logger.d("Failed to send launch crash reports within 2s timeout, continuing.", exc)
        } catch (exc: TimeoutException) {
            logger.d("Failed to send launch crash reports within 2s timeout, continuing.", exc)
        }
    }

    fun flushLaunchCrashReport() {
        val storedFiles = findStoredFiles()
        val launchCrashReport = findLaunchCrashReport(storedFiles)

        // cancel non-launch crash reports
        if (launchCrashReport != null) {
            storedFiles.remove(launchCrashReport)
        }
        cancelQueuedFiles(storedFiles)
        if (launchCrashReport != null) {
            logger.i("Attempting to send the most recent launch crash report")
            flushReports(listOf(launchCrashReport))
            logger.i("Continuing with Bugsnag initialisation")
        } else {
            logger.d("No startupcrash events to flush to Bugsnag.")
        }
    }

    fun findLaunchCrashReport(storedFiles: Collection<File>): File? {
        val launchCrashes: ArrayList<File?> = ArrayList()
        for (file in storedFiles) {
            val filenameInfo = fromFile(file, config)
            if (filenameInfo.isLaunchCrashReport()) {
                launchCrashes.add(file)
            }
        }

        // sort to get most recent timestamp
        launchCrashes.sortWith(EVENT_COMPARATOR)
        return if (launchCrashes.isEmpty()) null else launchCrashes[launchCrashes.size - 1]
    }

    fun writeAndDeliver(streamable: Streamable): Future<String>? {
        val filename = write(streamable)
        if (filename != null) {
            try {
                return bgTaskSevice.submitTask(
                    TaskType.ERROR_REQUEST,
                    Callable {
                        flushEventFile(File(filename))
                        filename
                    }
                )
            } catch (exception: RejectedExecutionException) {
                logger.w("Failed to flush all on-disk errors, retaining unsent errors for later.")
            }
        }
        return null
    }

    /**
     * Flush any on-disk errors to Bugsnag
     */
    fun flushAsync() {
        try {
            bgTaskSevice.submitTask(
                TaskType.ERROR_REQUEST,
                Runnable {
                    val storedFiles = findStoredFiles()
                    if (storedFiles.isEmpty()) {
                        logger.d("No regular events to flush to Bugsnag.")
                    }
                    flushReports(storedFiles)
                }
            )
        } catch (exception: RejectedExecutionException) {
            logger.w("Failed to flush all on-disk errors, retaining unsent errors for later.")
        }
    }

    fun flushReports(storedReports: Collection<File>) {
        if (!storedReports.isEmpty()) {
            val size = storedReports.size
            logger.i("Sending $size saved error(s) to Bugsnag")
            for (eventFile in storedReports) {
                flushEventFile(eventFile)
            }
        }
    }

    fun flushEventFile(eventFile: File) {
        try {
            val (apiKey) = fromFile(eventFile, config)
            val payload = createEventPayload(eventFile, apiKey)
            if (payload == null) {
                deleteStoredFiles(setOf(eventFile))
            } else {
                deliverEventPayload(eventFile, payload)
            }
        } catch (exception: Exception) {
            handleEventFlushFailure(exception, eventFile)
        }
    }

    private fun deliverEventPayload(eventFile: File, payload: EventPayload) {
        val deliveryParams = config.getErrorApiDeliveryParams(payload)
        val delivery = config.delivery
        val deliveryStatus = delivery.deliver(payload, deliveryParams)
        when (deliveryStatus) {
            DeliveryStatus.DELIVERED -> {
                deleteStoredFiles(setOf(eventFile))
                logger.i("Deleting sent error file " + eventFile.name)
            }
            DeliveryStatus.UNDELIVERED -> if (isTooBig(eventFile)) {
                logger.w(
                    "Discarding over-sized event (" + eventFile.length() + ") after failed delivery"
                )
                deleteStoredFiles(setOf(eventFile))
            } else if (isTooOld(eventFile)) {
                logger.w(
                    "Discarding historical event (from " + getCreationDate(eventFile) + ") after failed delivery"
                )
                deleteStoredFiles(setOf(eventFile))
            } else {
                cancelQueuedFiles(setOf(eventFile))
                logger.w(
                    "Could not send previously saved error(s)" + " to Bugsnag, will try again later"
                )
            }
            DeliveryStatus.FAILURE -> {
                val exc: Exception = RuntimeException("Failed to deliver event payload")
                handleEventFlushFailure(exc, eventFile)
            }
        }
    }

    private fun createEventPayload(eventFile: File, apiKey: String): EventPayload? {
        var apiKey: String? = apiKey
        val eventSource = MarshalledEventSource(eventFile, apiKey!!, logger)
        try {
            if (!callbackState.runOnSendTasks(eventSource, logger)) {
                // do not send the payload at all, we must block sending
                return null
            }
        } catch (ioe: Exception) {
            eventSource.clear()
        }
        val processedEvent = eventSource.event
        return if (processedEvent != null) {
            apiKey = processedEvent.apiKey
            EventPayload(apiKey, processedEvent, null, notifier, config)
        } else {
            EventPayload(apiKey, null, eventFile, notifier, config)
        }
    }

    private fun handleEventFlushFailure(exc: Exception, eventFile: File) {
        delegate?.onErrorIOFailure(exc, eventFile, "Crash Report Deserialization")
        deleteStoredFiles(setOf(eventFile))
    }

    override fun getFilename(`object`: Any?): String {
        val eventInfo = `object`?.let { fromEvent(obj = it, apiKey = null, config = config) }
        if (eventInfo != null) {
            return eventInfo.encode()
        }
        return ""
    }

    fun getNdkFilename(`object`: Any?, apiKey: String?): String {
        val eventInfo = fromEvent(obj = `object`!!, apiKey = apiKey, config = config)
        return eventInfo.encode()
    }

    init {
        this.logger = logger
        this.delegate = delegate
        this.notifier = notifier
        this.bgTaskSevice = bgTaskSevice
        this.callbackState = callbackState
    }

    fun isTooBig(file: File): Boolean {
        return file.length() > oneMegabyte
    }

    fun isTooOld(file: File?): Boolean {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DATE, -60)
        return findTimestampInFilename(file!!) < cal.timeInMillis
    }

    fun getCreationDate(file: File?): Date {
        return Date(findTimestampInFilename(file!!))
    }

    companion object {
        private const val LAUNCH_CRASH_TIMEOUT_MS: Long = 2000
        val EVENT_COMPARATOR: Comparator<in File?> = Comparator { lhs, rhs ->
            if (lhs == null && rhs == null) {
                return@Comparator 0
            }
            if (lhs == null) {
                return@Comparator 1
            }
            if (rhs == null) {
                -1
            } else lhs.compareTo(rhs)
        }
        private const val oneMegabyte = (1024 * 1024).toLong()
    }
}
