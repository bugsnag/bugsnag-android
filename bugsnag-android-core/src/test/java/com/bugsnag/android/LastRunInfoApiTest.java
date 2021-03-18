package com.bugsnag.android;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class LastRunInfoApiTest {

    /**
     * Verifies the LastRunInfo class contains the expected methods and hasn't broken
     * its API contract.
     */
    @Test
    public void testLastRunInfo() {
        LastRunInfo lastRunInfo = new LastRunInfo(5, true, false);
        assertEquals(5, lastRunInfo.getConsecutiveLaunchCrashes());
        assertTrue(lastRunInfo.getCrashed());
        assertFalse(lastRunInfo.getCrashedDuringLaunch());
    }
}
