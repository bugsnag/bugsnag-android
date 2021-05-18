package com.bugsnag.android;

import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.when;

import android.content.Context;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ForegroundDetectorInitTest {

    @Mock
    Context context;

    @Test
    public void safeInitNullActivityManager() {
        ForegroundDetector foregroundDetector = new ForegroundDetector(context);
        assertNull(foregroundDetector.isInForeground());
    }

    @Test
    public void safeInitSecurityException() {
        when(context.getSystemService(Context.ACTIVITY_SERVICE)).thenThrow(new SecurityException());

        ForegroundDetector foregroundDetector = new ForegroundDetector(context);
        assertNull(foregroundDetector.isInForeground());
    }
}
