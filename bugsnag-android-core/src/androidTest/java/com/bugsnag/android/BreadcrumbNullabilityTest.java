package com.bugsnag.android;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import androidx.test.filters.SmallTest;
import org.junit.Test;

import java.util.Date;

@SmallTest
public class BreadcrumbNullabilityTest {

    @Test
    public void testCreateFullBreadcrumbWithoutMetadata() {
        Date now = new Date();
        Breadcrumb crumb = new Breadcrumb("badger spotted", BreadcrumbType.USER, null, now, NoopLogger.INSTANCE);
        assertEquals("badger spotted", crumb.getMessage());
        assertEquals(BreadcrumbType.USER, crumb.getType());
        assertEquals(now, crumb.getTimestamp());
        assertNull(crumb.getMetadata());
    }
}
