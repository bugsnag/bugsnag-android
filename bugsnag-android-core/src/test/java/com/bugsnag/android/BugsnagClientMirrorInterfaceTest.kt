package com.bugsnag.android

import org.junit.Assert.assertTrue
import org.junit.Test
import java.lang.reflect.Method

/**
 * Uses reflection to check that the [Bugsnag] and [Client] classes share the same method
 * signatures, and fails if the two are out of sync.
 *
 * This is intended to catch the case where a new API is added to one class but not the other.
 * If a method genuinely only needs to be present on one of the classes, it should be added to
 * [bugsnagAllowList] or [clientAllowList].
 */
class BugsnagClientMirrorInterfaceTest {

    private val bugsnagAllowList = setOf(
        "start",
        "getClient",
        "isStarted"
    )

    private val clientAllowList = setOf(
        "update",
        "notifyObservers",
        "addObserver",
        "deleteObserver",
        "deleteObservers",
        "hasChanged",
        "countObservers"
    )

    data class MethodInfo(val name: String, val returnType: Class<*>, val params: List<Class<*>>) {
        constructor(m: Method) : this(m.name, m.returnType, m.parameterTypes.toList())
    }

    private val bugsnagMethods = Bugsnag::class.java.methods.map(::MethodInfo)
    private val clientMethods = Client::class.java.methods.map(::MethodInfo)

    @Test
    fun bugsnagHasSameMethodsAsClient() {
        val methods = clientMethods.subtract(bugsnagMethods)
            .filter { !clientAllowList.contains(it.name) }
        assertTrue("Bugsnag is missing the following methods: $methods", methods.isEmpty())
    }

    @Test
    fun clientHasSameMethodsAsBugsnag() {
        val methods = bugsnagMethods.subtract(clientMethods)
            .filter { !bugsnagAllowList.contains(it.name) }
        assertTrue("Client is missing the following methods: $methods", methods.isEmpty())
    }
}
