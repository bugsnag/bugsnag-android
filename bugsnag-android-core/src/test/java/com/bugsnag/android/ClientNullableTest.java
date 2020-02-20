package com.bugsnag.android;

import static com.bugsnag.android.BugsnagTestUtils.generateConfiguration;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Test;

import java.lang.IllegalArgumentException;

import java.util.Collections;
import java.util.Set;

public class ClientNullableTest {

    private Client client;

    // @Before
    // public void setup() {
    //     client = new Client(this, generateConfiguration());
    // }

    // @Test
    // public void testLeaveBreadcrumbNull() {
    //     client.leaveBreadcrumb(null);
    // }
}
