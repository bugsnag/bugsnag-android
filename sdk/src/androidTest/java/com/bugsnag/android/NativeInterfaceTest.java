package com.bugsnag.android;

import static org.junit.Assert.assertNotSame;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class NativeInterfaceTest {

    private Client client;

    @Before
    public void setUp() throws Exception {
        client = BugsnagTestUtils.generateClient();
    }

    @After
    public void tearDown() {
        client.close();
    }

    @Test
    public void getMetaData() {
        NativeInterface.setClient(client);
        assertNotSame(client.config.getMetaData().store, NativeInterface.getMetaData());
    }

}
