# Restore foreground tests after PLAT-1826
Feature: Synchronizing app/device metadata in the native layer

    Changes in the foreground state and rotation direction should be
    reflected in native crash and notify() reports.

#    Scenario: Capture foreground state while in the foreground
#        When I run "CXXDelayedNotifyScenario"
#        And I wait a bit
#        Then the request payload contains a completed native report
#        And the event "app.inForeground" is true
#        And the event "app.durationInForeground" is greater than 0
#        And the event "app.duration" is greater than 0
#        And the event "context" equals "MainActivity"
#        And the event "unhandled" is false
#
#    Scenario: Capture foreground state while in the background
#        When I run "CXXDelayedNotifyScenario" and press the home button
#        And I wait a bit
#        Then the request payload contains a completed native report
#        And the event "app.inForeground" is false
#        And the event "app.durationInForeground" equals 0
#        And the event "app.duration" is greater than 0
#        And the event "context" string is empty
#        And the event "unhandled" is false
#
#    Scenario: Capture foreground state while in a foreground crash
#        When I run "CXXTrapScenario"
#        And I wait a bit
#        And I wait for 2 seconds
#        And I relaunch the app
#        Then the request payload contains a completed native report
#        And the event "app.inForeground" is true
#        And the event "app.durationInForeground" is greater than 0
#        And the event "app.durationInForeground" is not null
#        And the event "app.duration" is not null
#        And the event "unhandled" is true
#
#    Scenario: Capture foreground state while in a background crash
#        When I run "CXXDelayedCrashScenario" and press the home button
#        And I wait a bit
#        And I wait for 2 seconds
#        And I relaunch the app
#        Then the request payload contains a completed native report
#        And the event "app.inForeground" is false
#        And the event "app.durationInForeground" equals 0
#        And the event "app.duration" is greater than 0
#        And the event "context" string is empty
#        And the event "unhandled" is true
#
