package com.bugsnag.android;

public class AppStateTest extends BugsnagTestCase {
    public void testSaneValues() {
        AppState appState = new AppState(getContext());

        assertTrue(appState.getMemoryUsage() > 0);
        assertNotNull(appState.isLowMemory());
        // TODO
        // assertNotNull(appState.getActiveScreen());
        // assertNotNull(appState.isInForeground());
    }
}
