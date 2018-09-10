Feature: Synchronizing app/device metadata in the native layer

    Scenario: Capture foreground state while in the foreground
        When I run "CXXDelayedNotifyScenario"
        And I wait a bit
        Then the request is a valid for the error reporting API
        And the event "app.inForeground" is true
        And the event "context" equals "MainActivity"

    Scenario: Capture foreground state while in the background
        When I run "CXXBackgroundNotifyScenario"
        And I wait a bit
        Then the request is a valid for the error reporting API
        And the event "app.inForeground" is false
        And the event "context" string is empty

    Scenario: Capture foreground state while in a foreground crash
        When I run "CXXTrapScenario"
        And I wait a bit
        And I configure the app to run in the "non-crashy" state
        And I relaunch the app
        Then the request is a valid for the error reporting API
        And the event "app.inForeground" is true

    Scenario: Capture foreground state while in a background crash
        When I run "CXXBackgroundCrashScenario"
        And I wait a bit
        And I configure the app to run in the "non-crashy" state
        And I relaunch the app
        Then the request is a valid for the error reporting API
        And the event "app.inForeground" is false
        And the event "context" string is empty
