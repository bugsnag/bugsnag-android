package com.bugsnag.android.internal.dag

import android.os.Looper
import java.util.concurrent.atomic.AtomicInteger

interface Provider<E> {
    fun getOrNull(): E?
    fun get(): E
}

abstract class RunnableProvider<E> : Provider<E>, Runnable {
    private val state = AtomicInteger(TASK_STATE_PENDING)

    @Volatile
    private var value: Any? = null

    abstract operator fun invoke(): E

    /**
     * Wait until this task has completed and return its value (or null if there was an error).
     */
    override fun getOrNull(): E? {
        return getOr { return null }
    }

    override fun get(): E {
        return getOr { throw value as Throwable }
    }

    private inline fun getOr(failureHandler: () -> E): E {
        while (true) {
            when (state.get()) {
                TASK_STATE_RUNNING -> awaitResult()
                TASK_STATE_PENDING -> {
                    if (isMainThread()) {
                        awaitResult()
                    } else {
                        run()
                    }
                }

                TASK_STATE_COMPLETE -> @Suppress("UNCHECKED_CAST") return value as E
                TASK_STATE_FAILED -> failureHandler()
            }
        }
    }

    private fun isMainThread(): Boolean {
        return Thread.currentThread() === mainThread
    }

    private fun awaitResult() {
        while (!isComplete()) {
            synchronized(this) {
                @Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
                (this as Object).wait()
            }
        }
    }

    private fun isComplete() = when (state.get()) {
        TASK_STATE_PENDING, TASK_STATE_RUNNING -> false
        else -> true
    }

    final override fun run() {
        if (state.compareAndSet(TASK_STATE_PENDING, TASK_STATE_RUNNING)) {
            try {
                value = invoke()
                state.set(TASK_STATE_COMPLETE)
            } catch (ex: Throwable) {
                value = ex
                state.set(TASK_STATE_FAILED)
            } finally {
                synchronized(this) {
                    // wakeup any waiting threads
                    @Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
                    (this as Object).notifyAll()
                }
            }
        }
    }

    companion object {
        const val TASK_STATE_PENDING = 0
        const val TASK_STATE_RUNNING = 1
        const val TASK_STATE_COMPLETE = 2
        const val TASK_STATE_FAILED = 999

        private val mainThread = Looper.getMainLooper().thread
    }
}
