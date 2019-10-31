package com.bugsnag.android

import java.io.IOException
import java.io.StringWriter
import java.util.Observable
import java.util.Queue
import java.util.concurrent.ConcurrentLinkedQueue

internal class Breadcrumbs(maxBreadcrumbs: Int, private val logger: Logger) : Observable(),
    JsonStream.Streamable {
    val store: Queue<Breadcrumb> = ConcurrentLinkedQueue()

    private val maxBreadcrumbs: Int

    init {
        when {
            maxBreadcrumbs > 0 -> this.maxBreadcrumbs = maxBreadcrumbs
            else -> this.maxBreadcrumbs = 0
        }
    }

    @Throws(IOException::class)
    override fun toStream(writer: JsonStream) {
        pruneBreadcrumbs()
        writer.beginArray()
        store.forEach { it.toStream(writer) }
        writer.endArray()
    }

    fun add(breadcrumb: Breadcrumb) {
        try {
            if (payloadSize(breadcrumb) > MAX_PAYLOAD_SIZE) {
                logger.w("Dropping breadcrumb because payload exceeds 4KB limit")
                return
            }
            store.add(breadcrumb)
            pruneBreadcrumbs()

            setChanged()
            notifyObservers(
                NativeInterface.Message(
                    NativeInterface.MessageType.ADD_BREADCRUMB, breadcrumb
                )
            )
        } catch (ex: IOException) {
            logger.w("Dropping breadcrumb because it could not be serialized", ex)
        }

    }

    fun clear() {
        store.clear()
        setChanged()
        notifyObservers(
            NativeInterface.Message(NativeInterface.MessageType.CLEAR_BREADCRUMBS, null)
        )
    }

    private fun pruneBreadcrumbs() {
        // Remove oldest breadcrumbs until new max size reached
        while (store.size > maxBreadcrumbs) {
            store.poll()
        }
    }

    companion object {
        private const val MAX_PAYLOAD_SIZE = 4096

        @Throws(IOException::class)
        internal fun payloadSize(breadcrumb: Breadcrumb): Int {
            val writer = StringWriter()
            val jsonStream = JsonStream(writer)
            breadcrumb.toStream(jsonStream)
            return writer.toString().length
        }
    }
}
