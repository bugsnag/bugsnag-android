Feature: NDK Session Tracking

Scenario: Paused session is not in payload of unhandled NDK error
    And I run "CXXPausedSessionScenario" and relaunch the app
    And I configure the app to run in the "non-crashy" state
    And I configure Bugsnag for "CXXPausedSessionScenario"
    And I wait to receive a session
    Then the session is valid for the session reporting API version "1.0" for the "Android Bugsnag Notifier" notifier

    And I wait to receive an error
    And the error is valid for the error reporting API version "4.0" for the "Android Bugsnag Notifier" notifier
    And the error payload field "events.0.session" is null

Scenario: Started session is in payload of unhandled NDK error
    And I run "CXXStartSessionScenario" and relaunch the app
    And I configure the app to run in the "non-crashy" state
    And I configure Bugsnag for "CXXStartSessionScenario"
    And I wait to receive a session
    Then the session is valid for the session reporting API version "1.0" for the "Android Bugsnag Notifier" notifier

    And I wait to receive an error
    And the error is valid for the error reporting API version "4.0" for the "Android Bugsnag Notifier" notifier
    And the error payload field "events.0.session.events.unhandled" equals 1
