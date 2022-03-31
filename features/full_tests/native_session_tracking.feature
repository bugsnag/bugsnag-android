Feature: NDK Session Tracking

  Background:
    Given I clear all persistent data

  Scenario: Paused session is not in payload of unhandled NDK error
    And I run "CXXPausedSessionScenario" and relaunch the crashed app
    And I configure Bugsnag for "CXXPausedSessionScenario"
    And I wait to receive a session
    Then the session is valid for the session reporting API version "1.0" for the "Android Bugsnag Notifier" notifier

    And I wait to receive an error
    And the error is valid for the error reporting API version "4.0" for the "Android Bugsnag Notifier" notifier
    And the error payload field "events.0.session" is null

  Scenario: Started session is in payload of unhandled NDK error
    And I run "CXXStartSessionScenario" and relaunch the crashed app
    And I configure Bugsnag for "CXXStartSessionScenario"
    And I wait to receive a session
    Then the session is valid for the session reporting API version "1.0" for the "Android Bugsnag Notifier" notifier

    And I wait to receive an error
    And the error is valid for the error reporting API version "4.0" for the "Android Bugsnag Notifier" notifier
    And the error payload field "events.0.session.events.unhandled" equals 1

  Scenario: Starting a session, notifying, followed by a C crash
    When I run "CXXSessionInfoCrashScenario" and relaunch the crashed app
    And I configure Bugsnag for "CXXSessionInfoCrashScenario"
    And I wait to receive a session
    And I wait to receive 3 errors
    And I discard the oldest error
    And I discard the oldest error
    Then the error payload contains a completed handled native report
    And the event contains session info
    And the error payload field "events.0.session.events.unhandled" equals 1
    And the error payload field "events.0.session.events.handled" equals 2
