package com.bugsnag.android

import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.junit.MockitoJUnitRunner

internal class UserStateTest {

    lateinit var state: UserState

    @Before
    fun setUp() {
        state = UserState(User("123", "j@bugsnag.com", "Jamie"))
    }

    @Test
    fun getUser() {
        assertEquals("123", state.user.id)
        assertEquals("j@bugsnag.com", state.user.email)
        assertEquals("Jamie", state.user.name)
    }

    @Test
    fun setUser() {
        val msgs = mutableListOf<StateEvent>()
        state.addObserver { _, arg ->
            msgs.add(arg as StateEvent)
        }

        state.user = User("99", "tc@example.com", "Tobias")

        val msg = msgs[0] as StateEvent.UpdateUser
        assertEquals("99", msg.user.id)
        assertEquals("tc@example.com", msg.user.email)
        assertEquals("Tobias", msg.user.name)
    }
}
