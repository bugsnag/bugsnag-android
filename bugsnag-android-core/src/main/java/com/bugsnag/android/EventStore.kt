package com.bugsnag.android

import android.os.SystemClock
import androidx.annotation.VisibleForTesting
import com.bugsnag.android.EventFilenameInfo.Companion.findTimestampInFilename
import com.bugsnag.android.EventFilenameInfo.Companion.fromEvent
import com.bugsnag.android.JsonStream.Streamable
import com.bugsnag.android.internal.BackgroundTaskService
import com.bugsnag.android.internal.FileQueue
import com.bugsnag.android.internal.ForegroundDetector
import com.bugsnag.android.internal.ImmutableConfig
import com.bugsnag.android.internal.TaskType
import com.bugsnag.android.internal.WriteErrorHandler
import java.io.File
import java.util.Calendar
import java.util.Date
import java.util.concurrent.Callable
import java.util.concurrent.ExecutionException
import java.util.concurrent.Future
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

/**
 * Store and flush Event reports.
 */
internal class EventStore(
    private val config: ImmutableConfig,
    private val logger: Logger,
    private val notifier: Notifier,
    private val bgTaskService: BackgroundTaskService,
    writeErrorHandler: WriteErrorHandler?,
    private val callbackState: CallbackState
) {
    var onEventStoreEmptyCallback: () -> Unit = {}
    var onDiscardEventCallback: (EventPayload) -> Unit = {}
    private var isEmptyEventCallbackCalled: Boolean = false

    private val queue = FileQueue(
        File(config.persistenceDirectory.value, "bugsnag/errors"),
        config.maxPersistedEvents,
        logger,
        EVENT_COMPARATOR,
        writeErrorHandler
    ) {
        if (!isEmptyEventCallbackCalled) {
            onEventStoreEmptyCallback()
            isEmptyEventCallbackCalled = true
        }
    }

    val storageDir: File by queue::storageDir

    /**
     * Flush startup crashes synchronously on the main thread. Startup crashes block the main thread
     * when being sent (subject to [Configuration.setSendLaunchCrashesSynchronously])
     */
    fun flushOnLaunch() {
        if (!config.sendLaunchCrashesSynchronously) {
            return
        }
        val future = try {
            bgTaskService.submitTask(TaskType.ERROR_REQUEST) {
                flushLaunchCrashReport()
            }
        } catch (exc: RejectedExecutionException) {
            logger.d("Failed to flush launch crash reports, continuing.", exc)
            return
        }
        try {
            // Calculate the maximum amount of time we are prepared to block while sending
            // startup crashes, based on how long we think startup has taken so-far.
            // This attempts to mitigate possible startup ANRs that can occur when other SDKs
            // have blocked the main thread before this code is reached.
            val currentStartupDuration =
                SystemClock.elapsedRealtime() - ForegroundDetector.startupTime
            var timeout = LAUNCH_CRASH_TIMEOUT_MS - currentStartupDuration

            if (timeout <= 0) {
                // if Bugsnag.start is called too long after Application.onCreate is expected to
                // have returned, we use a full LAUNCH_CRASH_TIMEOUT_MS instead of a calculated one
                // assuming that the app is already fully started
                timeout = LAUNCH_CRASH_TIMEOUT_MS
            }

            future.get(timeout, TimeUnit.MILLISECONDS)
        } catch (exc: InterruptedException) {
            logger.d("Failed to send launch crash reports within timeout, continuing.", exc)
        } catch (exc: ExecutionException) {
            logger.d("Failed to send launch crash reports within timeout, continuing.", exc)
        } catch (exc: TimeoutException) {
            logger.d("Failed to send launch crash reports within timeout, continuing.", exc)
        }
    }

    private fun flushLaunchCrashReport() {
        val startupCrashProcessed = queue.processLastFile(this::isLaunchCrash) { report ->
            logger.i("Attempting to send the most recent launch crash report")
            flushEventFile(report)
            logger.i("Continuing with Bugsnag initialisation")
        }

        if (!startupCrashProcessed) {
            logger.d("No startupcrash events to flush to Bugsnag.")
        }
    }

    fun write(event: Streamable): File? {
        return queue.write(getFilename(event), event)
    }

    fun enqueueContentForDelivery(payload: String, filename: String) {
        queue.write(filename) { out ->
            out.write(payload)
        }
    }

    fun writeAndDeliver(streamable: Streamable): Future<String>? {
        val file = queue.write(getFilename(streamable), streamable) ?: return null
        try {
            return bgTaskService.submitTask(
                TaskType.ERROR_REQUEST,
                Callable {
                    flushEventFile(file)
                    file.absolutePath
                }
            )
        } catch (exception: RejectedExecutionException) {
            logger.w("Failed to flush all on-disk errors, retaining unsent errors for later.")
        }
        return null
    }

    /**
     * Flush any on-disk errors to Bugsnag
     */
    fun flushAsync() {
        try {
            bgTaskService.submitTask(TaskType.ERROR_REQUEST) {
                if (queue.isEmpty()) {
                    logger.d("No regular events to flush to Bugsnag.")
                }
                flushReports()
            }
        } catch (exception: RejectedExecutionException) {
            logger.w("Failed to flush all on-disk errors, retaining unsent errors for later.")
        }
    }

    fun isEmpty() = queue.isEmpty()

    private fun flushReports() {
        queue.processEnqueuedFiles { storedReports ->
            val size = storedReports.size
            logger.i("Sending $size saved error(s) to Bugsnag")

            for (eventFile in storedReports) {
                flushEventFile(eventFile)
            }
        }
    }

    private fun flushEventFile(eventFile: File) {
        try {
            val (apiKey) = EventFilenameInfo.fromFile(eventFile, config)
            val payload = createEventPayload(eventFile, apiKey)
            if (payload != null) {
                deliverEventPayload(eventFile, payload)
            }
        } catch (exception: Exception) {
            handleEventFlushFailure(exception, eventFile)
        }
    }

    private fun deliverEventPayload(eventFile: File, payload: EventPayload) {
        val deliveryParams = config.getErrorApiDeliveryParams(payload)
        val delivery = config.delivery
        when (delivery.deliver(payload, deliveryParams)) {
            DeliveryStatus.DELIVERED -> {
                queue.delete(eventFile)
                logger.i("Deleting sent error file $eventFile.name")
            }

            DeliveryStatus.UNDELIVERED -> undeliveredEventPayload(eventFile)
            DeliveryStatus.FAILURE -> {
                val exc: Exception = RuntimeException("Failed to deliver event payload")
                handleEventFlushFailure(exc, eventFile)
            }
        }
    }

    private fun undeliveredEventPayload(eventFile: File) {
        if (isTooBig(eventFile)) {
            logger.w(
                "Discarding over-sized event (${eventFile.length()}) after failed delivery"
            )
            discardEvents(eventFile)
            queue.delete(eventFile)
        } else if (isTooOld(eventFile)) {
            logger.w(
                "Discarding historical event (from ${getCreationDate(eventFile)}) after failed delivery"
            )
            discardEvents(eventFile)
            queue.delete(eventFile)
        } else {
            logger.w(
                "Could not send previously saved error(s) to Bugsnag, will try again later"
            )
        }
    }

    private fun createEventPayload(eventFile: File, apiKey: String): EventPayload? {
        @Suppress("NAME_SHADOWING")
        var apiKey: String? = apiKey
        val eventSource = MarshalledEventSource(eventFile, apiKey!!, logger)
        try {
            if (!callbackState.runOnSendTasks(eventSource, logger)) {
                // do not send the payload at all, we must block sending
                return null
            }
        } catch (ioe: Exception) {
            logger.w("could not parse event payload", ioe)
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
        logger.e(exc.message ?: "Failed to send event", exc)
        queue.delete(eventFile)
    }

    @VisibleForTesting
    internal fun isLaunchCrash(file: File): Boolean {
        return EventFilenameInfo.fromFile(file, config).isLaunchCrashReport()
    }

    private fun getFilename(obj: Any?): String {
        return obj?.let { fromEvent(obj = it, apiKey = null, config = config) }?.encode() ?: ""
    }

    fun getNdkFilename(obj: Any?, apiKey: String?): String {
        return obj?.let { fromEvent(obj = it, apiKey = apiKey, config = config) }?.encode() ?: ""
    }

    private fun isTooBig(file: File): Boolean {
        return file.length() > oneMegabyte
    }

    private fun isTooOld(file: File): Boolean {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DATE, -60)
        return findTimestampInFilename(file) < cal.timeInMillis
    }

    private fun getCreationDate(file: File): Date {
        return Date(findTimestampInFilename(file))
    }

    private fun discardEvents(eventFile: File) {
        val eventFilenameInfo = EventFilenameInfo.fromFile(eventFile, config)
        onDiscardEventCallback(
            EventPayload(
                eventFilenameInfo.apiKey,
                null,
                eventFile,
                notifier,
                config
            )
        )
    }

    companion object {
        private const val LAUNCH_CRASH_TIMEOUT_MS: Long = 2000
        private const val oneMegabyte = 1024L * 1024L

        val EVENT_COMPARATOR: Comparator<in File?> = Comparator { lhs, rhs ->
            when {
                lhs == null && rhs == null -> 0
                lhs == null -> 1
                rhs == null -> -1
                else -> lhs.compareTo(rhs)
            }
        }
    }
}
