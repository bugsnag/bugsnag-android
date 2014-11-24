package com.bugsnag.android;

import com.bugsnag.android.AppState;

public class AppStateTest extends BugsnagTestCase {
    public void testSaneValues() {
        AppState appState = new AppState(getContext());

        assertTrue(appState.getMemoryUsage() > 0);
        assertNotNull(appState.isLowMemory());
        assertNotNull(appState.getActiveScreen());
        assertNotNull(appState.isInForeground());
    }
}
