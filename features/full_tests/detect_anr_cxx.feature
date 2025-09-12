Feature: ANRs triggered in CXX code are captured

  Background:
    Given I clear all persistent data

  # PLAT-9580
  @skip_bitbar
  Scenario: ANR triggered in CXX code is captured
    When I run "CXXAnrScenario"
    And I wait for 2 seconds
    And I cause the ANR dialog to appear
    Then I wait to receive an error
    And the error is valid for the error reporting API version "4.0" for the "Android Bugsnag Notifier" notifier
    And the exception "errorClass" equals "ANR"
    And the exception "message" starts with " Input dispatching timed out"
    And the error "Bugsnag-Stacktrace-Types" header equals "android,c"
    And the error payload field "events.0.exceptions.0.type" equals "android"
    And the error payload field "events.0.exceptions.0.stacktrace.0.type" equals "c"
    And the error payload field "events.0.threads.0.type" equals "android"
    And the error payload field "events.0.threads.0.stacktrace.0.type" is null

  # PLAT-9580
  @skip_bitbar
  Scenario: ANR triggered in CXX code is captured even when NDK detection is disabled
    When I run "CXXAnrNdkDisabledScenario"
    And I wait for 2 seconds
    And I cause the ANR dialog to appear
    Then I wait to receive an error
    And the error is valid for the error reporting API version "4.0" for the "Android Bugsnag Notifier" notifier
    And the exception "errorClass" equals "ANR"
    And the exception "message" starts with " Input dispatching timed out"
    And the error "Bugsnag-Stacktrace-Types" header equals "android"
    And the error payload field "events.0.exceptions.0.type" equals "android"
    And the error payload field "events.0.exceptions.0.stacktrace.0.type" is null
    And the error payload field "events.0.threads.0.type" equals "android"
    And the error payload field "events.0.threads.0.stacktrace.0.type" is null
