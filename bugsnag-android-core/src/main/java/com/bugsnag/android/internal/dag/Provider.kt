package com.bugsnag.android.internal.dag

import android.os.Looper
import java.util.concurrent.atomic.AtomicInteger

/**
 * A lightweight abstraction similar to `Lazy` or `Future` allowing values to be calculated on
 * separate threads, or to be pre-computed.
 */
interface Provider<E> {
    /**
     * Same as [get] but will return `null` instead of throwing an exception if the value could
     * not be computed.
     */
    fun getOrNull(): E?

    /**
     * Return the value sourced from this provider, throwing an exception if the provider failed
     * to calculate a value. Anything thrown from here will have been captured when attempting
     * to calculate the value.
     */
    fun get(): E
}

/**
 * The primary implementation of [Provider], usually created using the
 * [BackgroundDependencyModule.provider] function. Similar conceptually to
 * [java.util.concurrent.FutureTask] but with a more compact implementation. The implementation
 * of [RunnableProvider.get] is special because it behaves more like [Lazy.value] in that getting
 * a value that is still pending will cause it to be run on the current thread instead of waiting
 * for it to be run "sometime in the future". This makes RunnableProviders less bug-prone when
 * dealing with single-thread executors (such as those in [BackgroundTaskService]). RunnableProvider
 * also has special handling for the main-thread, ensuring no computational work (such as IO) is
 * done on the main thread.
 */
abstract class RunnableProvider<E> : Provider<E>, Runnable {
    private val state = AtomicInteger(TASK_STATE_PENDING)

    @Volatile
    private var value: Any? = null

    abstract operator fun invoke(): E

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
        synchronized(this) {
            while (!isComplete()) {
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

    private companion object {
        private const val TASK_STATE_PENDING = 0
        private const val TASK_STATE_RUNNING = 1
        private const val TASK_STATE_COMPLETE = 2
        private const val TASK_STATE_FAILED = 999

        private val mainThread = Looper.getMainLooper().thread
    }
}
