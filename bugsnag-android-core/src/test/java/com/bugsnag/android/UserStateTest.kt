package com.bugsnag.android

import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
internal class UserStateTest {

    @Mock
    lateinit var repository: UserRepository

    @Before
    fun setUp() {
        `when`(repository.load()).thenReturn(User("123", "j@bugsnag.com", "Jamie"))
    }

    @Test
    fun getUser() {
        val state = UserState(repository)
        assertEquals("123", state.user.id)
        assertEquals("j@bugsnag.com", state.user.email)
        assertEquals("Jamie", state.user.name)
    }

    @Test
    fun setUserId() {
        val state = UserState(repository)
        var msg: StateEvent.UpdateUserId? = null
        state.addObserver { _, arg ->
            msg = arg as StateEvent.UpdateUserId
        }
        state.setUserId("55")
        assertEquals("55", msg!!.id)
    }

    @Test
    fun setUserEmail() {
        val state = UserState(repository)
        var msg: StateEvent.UpdateUserEmail? = null
        state.addObserver { _, arg ->
            msg = arg as StateEvent.UpdateUserEmail
        }
        state.setUserEmail("woop@example.com")
        assertEquals("woop@example.com", msg!!.email)
    }

    @Test
    fun setUserName() {
        val state = UserState(repository)
        var msg: StateEvent.UpdateUserName? = null
        state.addObserver { _, arg ->
            msg = arg as StateEvent.UpdateUserName
        }
        state.setUserName("Foo")
        assertEquals("Foo", msg!!.name)
    }

    @Test
    fun setUser() {
        val state = UserState(repository)
        val msgs = mutableListOf<StateEvent>()
        state.addObserver { _, arg ->
            msgs.add(arg as StateEvent)
        }

        state.setUser("99", "tc@example.com", "Tobias")

        assertEquals("99", (msgs[0] as StateEvent.UpdateUserId).id)
        assertEquals("tc@example.com", (msgs[1] as StateEvent.UpdateUserEmail).email)
        assertEquals("Tobias", (msgs[2] as StateEvent.UpdateUserName).name)
    }
}
