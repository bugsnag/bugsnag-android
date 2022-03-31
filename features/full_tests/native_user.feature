Feature: Native User API

  Background:
    Given I clear all persistent data

  Scenario: Adding user information in Java followed by a C crash
    And I run "CXXJavaUserInfoNativeCrashScenario" and relaunch the crashed app
    And I configure Bugsnag for "CXXJavaUserInfoNativeCrashScenario"
    And I wait to receive an error
    Then the error payload contains a completed handled native report
    And the exception "errorClass" equals one of:
      | SIGILL  |
      | SIGTRAP |
    And the event "severity" equals "error"
    And the event "user.name" equals "Strulyegha  Ghaumon  Rabelban  Snefkal  Angengtai  Samperris  D"
    And the event "user.id" equals "9816734"
    And the event "user.email" equals "j@example.com"
    And the event "unhandled" is true

  Scenario: Set user in Native layer followed by a Java crash
    When I run "CXXNativeUserInfoJavaCrashScenario" and relaunch the crashed app
    And I configure Bugsnag for "CXXNativeUserInfoJavaCrashScenario"
    And I wait to receive an error
    Then the error is valid for the error reporting API version "4.0" for the "Android Bugsnag Notifier" notifier
    And the error payload field "events" is an array with 1 elements
    And the event "user.id" equals "24601"
    And the event "user.email" equals "test@test.test"
    And the event "user.name" equals "test user"
