package com.bugsnag.android;

import static org.junit.Assert.assertEquals;

import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Date;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class BreadcrumbNullabilityTest {

    @Test
    public void testCreateBreadcrumbWithoutMetadata() {
        Breadcrumb crumb = new Breadcrumb("badger spotted", BreadcrumbType.USER, null);
        assertEquals("badger spotted", crumb.getName());
        assertEquals(BreadcrumbType.USER, crumb.getType());
        assertEquals(0, crumb.getMetadata().size());
    }

    @Test
    public void testCreateFullBreadcrumbWithoutMetadata() {
        Breadcrumb crumb = new Breadcrumb("badger spotted", BreadcrumbType.USER, new Date(), null);
        assertEquals("badger spotted", crumb.getName());
        assertEquals(BreadcrumbType.USER, crumb.getType());
        assertEquals(0, crumb.getMetadata().size());
    }
}
