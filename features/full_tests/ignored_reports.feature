Feature: Reports are ignored

  Background:
    Given I clear all persistent data

  Scenario: Exception classname ignored
    When I run "IgnoredExceptionScenario"
    And I wait to receive an error
    Then the error is valid for the error reporting API version "4.0" for the "Android Bugsnag Notifier" notifier
    And the exception "errorClass" equals "java.lang.IllegalStateException"
    And the exception "message" equals "Is it me you're looking for?"
    And the event "unhandled" is false

  Scenario: Disabled Exception Handler
    When I run "DisableAutoDetectErrorsScenario" and relaunch the crashed app
    And I configure Bugsnag for "DisableAutoDetectErrorsScenario"
    Then Bugsnag confirms it has no errors to send

  Scenario: Changing release stage to exclude the current stage settings before a POSIX signal
    When I run "CXXTrapOutsideReleaseStagesScenario" and relaunch the crashed app
    And I configure Bugsnag for "CXXTrapOutsideReleaseStagesScenario"
    Then Bugsnag confirms it has no errors to send

  Scenario: Changing release stage settings to exclude the current stage before a native C++ crash
    When I run "CXXThrowSomethingOutsideReleaseStagesScenario" and relaunch the crashed app
    And I configure Bugsnag for "CXXThrowSomethingOutsideReleaseStagesScenario"
    Then Bugsnag confirms it has no errors to send

  Scenario: SIGABRT ignored
    When I run "CXXIgnoredSigabrtScenario"
    And I relaunch the app after a crash
    And I configure the app to run in the "SIGABRT" state
    And I configure Bugsnag for "CXXIgnoredSigabrtScenario"
    And I should receive no errors

  Scenario: SIGABRT not ignored
    When I run "CXXIgnoredSigabrtScenario"
    And I relaunch the app after a crash
    And I configure the app to run in the "SIGBUS" state
    And I configure Bugsnag for "CXXIgnoredSigabrtScenario"
    And I wait to receive an error
    Then the error is valid for the error reporting API version "4.0" for the "Android Bugsnag Notifier" notifier
    And the exception "errorClass" equals "SIGABRT"
