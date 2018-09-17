Feature: Synchronizing app/device metadata in the native layer

    Scenario: Capture foreground state while in the foreground
        When I run "CXXDelayedNotifyScenario"
        And I wait a bit
        Then the request payload contains a completed native report
        And the event "app.inForeground" is true
        And the event "app.duration" is greater than 0
        And the event "context" equals "MainActivity"
        And the event "unhandled" is false

    Scenario: Capture foreground state while in the background
        When I run "CXXDelayedNotifyScenario" and press the home button
        And I wait a bit
        Then the request payload contains a completed native report
        And the event "app.inForeground" is false
        And the event "app.durationInForeground" equals 0
        And the event "app.duration" is greater than 0
        And the event "context" string is empty
        And the event "unhandled" is false

    Scenario: Capture foreground state while in a foreground crash
        When I run "CXXTrapScenario"
        And I wait a bit
        And I configure the app to run in the "non-crashy" state
        And I relaunch the app
        Then the request payload contains a completed native report
        And the event "app.inForeground" is true
        And the event "app.durationInForeground" is not null
        And the event "app.duration" is not null
        And the event "unhandled" is true

    Scenario: Capture foreground state while in a background crash
        When I run "CXXDelayedCrashScenario" and press the home button
        And I wait a bit
        And I configure the app to run in the "non-crashy" state
        And I relaunch the app
        Then the request payload contains a completed native report
        And the event "app.inForeground" is false
        And the event "app.durationInForeground" equals 0
        And the event "app.duration" is greater than 0
        And the event "context" string is empty
        And the event "unhandled" is true

    Scenario Outline: Capture rotation and notify in C
        When I run "CXXDelayedNotifyScenario" in "<mode>" orientation
        And I wait a bit
        Then the request payload contains a completed native report
        And the event "device.orientation" equals "<orientation>"

        Examples:
        | mode            | orientation |
        | landscape-left  | landscape   |
        | landscape-right | landscape   |
        | upside-down     | portrait    |
        | portrait        | portrait    |

    Scenario Outline: Capture rotation and crash in C
        When I run "CXXDelayedCrashScenario" in "<mode>" orientation
        And I wait a bit
        And I configure the app to run in the "non-crashy" state
        And I relaunch the app
        Then the request payload contains a completed native report
        And the event "device.orientation" equals "<orientation>"

        Examples:
        | mode            | orientation |
        | landscape-left  | landscape   |
        | landscape-right | landscape   |
        | upside-down     | portrait    |
        | portrait        | portrait    |

