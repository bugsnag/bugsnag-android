Feature: Native API

    Scenario: Notifying in C
        When I run "CXXNotifyScenario"
        And I wait to receive an error
        Then the error payload contains a completed handled native report
        And the event "severity" equals "error"
        And the exception "errorClass" equals "Vitamin C deficiency"
        And the exception "message" equals "9 out of 10 adults do not get their 5-a-day"
        And the event "unhandled" is false

    @Flaky
    Scenario: Starting a session, notifying, followed by a C crash
        When I run "CXXSessionInfoCrashScenario" and relaunch the app
        And I configure the app to run in the "non-crashy" state
        And I configure Bugsnag for "CXXSessionInfoCrashScenario"
        And I wait to receive a session
        And I wait to receive 3 errors
        And I discard the oldest error
        And I discard the oldest error
        Then the error payload contains a completed handled native report
        And the event contains session info
        And the error payload field "events.0.session.events.unhandled" equals 1
        And the error payload field "events.0.session.events.handled" equals 2

    Scenario: Set extraordinarily long app information
        When I run "CXXExtraordinaryLongStringScenario" and relaunch the app
        And I configure the app to run in the "non-crashy" state
        And I configure Bugsnag for "CXXExtraordinaryLongStringScenario"
        And I wait to receive an error
        And the error payload contains a completed handled native report
        And the exception "errorClass" equals one of:
          | SIGILL |
          | SIGTRAP |
        And the event "app.version" equals "22.312.749.78.300.810.24.167.32"
        And the event "context" equals "ObservableSessionInitializerStringParserStringSessionProxyGloba"
        And the event "unhandled" is true

    Scenario: Use the NDK methods without "env" after calling "bugsnag_start"
        When I run "CXXStartScenario"
        And I wait to receive an error
        Then the error payload contains a completed handled native report
        And the event "unhandled" is false
        And the exception "errorClass" equals "Start scenario"
        And the exception "message" equals "Testing env"
        And the event "severity" equals "info"
        And the event has a "log" breadcrumb named "Start scenario crumb"
