Feature: Overriding unhandled state

  Background:
    Given I clear all persistent data

  Scenario: Non-fatal exception overridden to unhandled
    When I run "OverrideToUnhandledExceptionScenario"
    Then I wait to receive an error
    And I wait to receive a session
    And the error is valid for the error reporting API version "4.0" for the "Android Bugsnag Notifier" notifier
    And the exception "errorClass" equals "java.lang.RuntimeException"
    And the exception "message" equals "OverrideToUnhandledExceptionScenario"
    And the event "unhandled" is true
    And the event "severity" equals "warning"
    And the event "severityReason.type" equals "handledException"
    And the event "severityReason.unhandledOverridden" is true
    And the event "session.events.handled" equals 0
    And the event "session.events.unhandled" equals 1

  Scenario: Fatal exception overridden to handled
    When I run "OverrideToHandledExceptionScenario" and relaunch the crashed app
    And I configure Bugsnag for "OverrideToHandledExceptionScenario"
    And I wait to receive an error
    And I wait to receive a session
    And the error is valid for the error reporting API version "4.0" for the "Android Bugsnag Notifier" notifier
    And the exception "errorClass" equals "java.lang.RuntimeException"
    And the exception "message" equals "OverrideToHandledExceptionScenario"
    And the event "unhandled" is false
    And the event "severity" equals "error"
    And the event "severityReason.type" equals "unhandledException"
    And the event "severityReason.unhandledOverridden" is true
    And the event "session.events.handled" equals 1
    And the event "session.events.unhandled" equals 0

  Scenario: CXX error overridden to handled
    When I run "CXXHandledOverrideScenario" and relaunch the crashed app
    And I configure Bugsnag for "CXXHandledOverrideScenario"
    And I wait to receive an error
    And the error is valid for the error reporting API version "4.0" for the "Android Bugsnag Notifier" notifier
    And the exception "errorClass" equals "SIGABRT"
    And the exception "message" equals "Abort program"
    And the event "unhandled" is false
    And the event "severity" equals "error"
    And the event "severityReason.type" equals "signal"
    And the event "severityReason.attributes.signalType" equals "SIGABRT"
    And the event "severityReason.unhandledOverridden" is true
    And the event "session.events.handled" equals 1
    And the event "session.events.unhandled" equals 0
