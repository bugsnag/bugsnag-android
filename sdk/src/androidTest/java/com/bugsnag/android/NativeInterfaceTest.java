package com.bugsnag.android;

import static org.junit.Assert.assertNotSame;

import org.junit.Test;

public class NativeInterfaceTest {

    @Test
    public void getMetaData() {
        Client client = BugsnagTestUtils.generateClient();
        NativeInterface.setClient(client);
        assertNotSame(client.config.getMetaData().store, NativeInterface.getMetaData());
    }

}
