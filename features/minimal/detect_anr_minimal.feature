Feature: ANRs triggered in a fixture with only bugsnag-android-core are captured

  Background:
    Given I clear all persistent data

  @anr
  @skip_android_10
  Scenario: Triggering ANR does not crash the minimal app
    When I run "JvmAnrMinimalFixtureScenario"
    And I wait for 2 seconds
    And I tap the screen 3 times
    And I wait for 4 seconds
    Then I wait to receive an error
    And the error is valid for the error reporting API version "4.0" for the "Android Bugsnag Notifier" notifier
    And the exception "errorClass" equals "java.lang.RuntimeException"
    And the exception "message" starts with "JvmAnrMinimalFixtureScenario"
    And the error "Bugsnag-Stacktrace-Types" header equals "android"
    And the error payload field "events.0.exceptions.0.type" equals "android"
    And the error payload field "events.0.exceptions.0.stacktrace.0.type" is null
    And the error payload field "events.0.threads.0.type" equals "android"
    And the error payload field "events.0.threads.0.stacktrace.0.type" is null
