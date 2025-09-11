Feature: ANRs triggered in JVM code are captured

  Background:
    Given I clear all persistent data

  @anr
  @skip_samsung
  Scenario: ANR triggered in JVM loop code is captured
    When I clear any error dialogue
    And I run "JvmAnrLoopScenario"
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

  @anr
  @skip_samsung
  Scenario: ANR triggered in JVM sleep code is captured
    When I clear any error dialogue
    And I run "JvmAnrSleepScenario"
    And I wait for 2 seconds
    And I cause the ANR dialog to appear
    Then I wait to receive an error
    And the error is valid for the error reporting API version "4.0" for the "Android Bugsnag Notifier" notifier
    And the exception "errorClass" equals "ANR"
    And the exception "message" starts with " Input dispatching timed out"
    And the error "Bugsnag-Stacktrace-Types" header equals "android,c"
    And the error payload field "events.0.exceptions.0.type" equals "android"

  @anr
  @skip_samsung
  Scenario: ANR triggered in JVM code is not captured when detectAnrs = false
    When I run "JvmAnrDisabledScenario"
    And I wait for 2 seconds
    And I cause the ANR dialog to appear
    Then I wait to receive an error
    And the error is valid for the error reporting API version "4.0" for the "Android Bugsnag Notifier" notifier
    And the error payload field "events" is an array with 1 elements
    And the exception "errorClass" equals "java.lang.RuntimeException"
    And the exception "message" equals "JvmAnrDisabledScenario"

  @anr
  @skip_samsung
  Scenario: ANR triggered in JVM code is not captured when outside of release stage
    When I run "JvmAnrOutsideReleaseStagesScenario"
    And I wait for 2 seconds
    And I cause the ANR dialog to appear
    Then I should receive no errors
