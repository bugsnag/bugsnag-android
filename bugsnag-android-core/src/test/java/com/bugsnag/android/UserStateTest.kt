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
        var msg: NativeInterface.Message? = null
        state.addObserver { _, arg ->
            msg = arg as NativeInterface.Message
        }
        state.setUserId("55")

        assertEquals(NativeInterface.MessageType.UPDATE_USER_ID, msg!!.type)
        assertEquals("55", msg!!.value)
    }

    @Test
    fun setUserEmail() {
        val state = UserState(repository)
        var msg: NativeInterface.Message? = null
        state.addObserver { _, arg ->
            msg = arg as NativeInterface.Message
        }
        state.setUserEmail("woop@example.com")

        assertEquals(NativeInterface.MessageType.UPDATE_USER_EMAIL, msg!!.type)
        assertEquals("woop@example.com", msg!!.value)
    }

    @Test
    fun setUserName() {
        val state = UserState(repository)
        var msg: NativeInterface.Message? = null
        state.addObserver { _, arg ->
            msg = arg as NativeInterface.Message
        }
        state.setUserName("Foo")

        assertEquals(NativeInterface.MessageType.UPDATE_USER_NAME, msg!!.type)
        assertEquals("Foo", msg!!.value)
    }

    @Test
    fun setUser() {
        val state = UserState(repository)
        val msgs = mutableListOf<NativeInterface.Message>()
        state.addObserver { _, arg ->
            msgs.add(arg as NativeInterface.Message)
        }

        state.setUser("99", "tc@example.com", "Tobias")

        assertEquals(NativeInterface.MessageType.UPDATE_USER_ID, msgs[0].type)
        assertEquals("99", msgs[0].value)
        assertEquals(NativeInterface.MessageType.UPDATE_USER_EMAIL, msgs[1].type)
        assertEquals("tc@example.com", msgs[1].value)
        assertEquals(NativeInterface.MessageType.UPDATE_USER_NAME, msgs[2].type)
        assertEquals("Tobias", msgs[2].value)
    }
}
