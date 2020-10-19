package com.bugsnag.android;

import static org.mockito.Mockito.when;

import android.app.Application;
import android.content.Context;

import kotlin.TypeCastException;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;


/**
 * Verifies that the {@link Client} can be initialized with different subtypes
 * of {@link Context} without crashing. The recommended usage is to initialize Bugsnag in
 * {@link Application#onCreate()} which uses the application context, as this object remains
 * available for the entire process lifecycle.
 *
 * It is also possible to initialize with a short-lived {@link android.app.Activity} context,
 * from which the application context can be accessed via {@link Context#getApplicationContext()}.
 * Neglecting to do this is a memory leak as the Activity object would otherwise be garbage
 * collected.
 *
 * Finally, it is possible to initialize with the context passed as a parameter to
 * {@link Application#attachBaseContext(Context)}. Initializing here is preferable as it will
 * capture any crashes that occur before {@link Application#onCreate()} are called - for instance,
 * crashes within a {@link android.content.ContentProvider}.
 *
 * Because this method is called before {@link Application#onCreate()} the application context will
 * be null. This test verifies that initializing with all these context subtypes
 * does not throw an {@link IllegalArgumentException} due to the context being null. As the Client
 * relies on other Android framework classes that aren't accessible in a unit test that runs on
 * the JVM, we assert that another exception further into the Client constructor is thrown instead.
 */
@RunWith(MockitoJUnitRunner.class)
public class ClientContextInitTest {

    @Mock
    Context appContext;

    @Mock
    Context anotherContext;

    /**
     * Verifies that if the application context is null the base context is used
     */
    @Test(expected = TypeCastException.class)
    public void testClientBaseContext() {
        new Client(appContext, new Configuration("api-key"));
    }

    /**
     * Verifies that the application context is always used if it is not null
     */
    @Test(expected = TypeCastException.class)
    public void testClientAppContext() {
        when(appContext.getApplicationContext()).thenReturn(anotherContext);
        new Client(appContext, new Configuration("api-key"));
    }
}
