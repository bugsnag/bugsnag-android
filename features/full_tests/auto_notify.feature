Feature: Switching automatic error detection on/off for Unity

  Background:
    Given I clear all persistent data

  Scenario: Handled JVM exceptions are captured with autoNotify=false
    When I run "HandledJvmAutoNotifyFalseScenario"
    Then I wait to receive an error
    And the error is valid for the error reporting API version "4.0" for the "Android Bugsnag Notifier" notifier
    And the exception "message" equals "HandledJvmAutoNotifyFalseScenario"

  Scenario: JVM exception not captured with autoNotify=false
    When I run "UnhandledJvmAutoNotifyFalseScenario" and relaunch the crashed app
    And I configure Bugsnag for "UnhandledJvmAutoNotifyFalseScenario"
    Then Bugsnag confirms it has no errors to send

  Scenario: NDK signal not captured with autoNotify=false
    When I run "UnhandledNdkAutoNotifyFalseScenario" and relaunch the crashed app
    And I configure Bugsnag for "UnhandledNdkAutoNotifyFalseScenario"
    Then Bugsnag confirms it has no errors to send

  @skip_android_8_1
  Scenario: ANR not captured with autoDetectAnrs=false
    When I run "AutoDetectAnrsFalseScenario" and relaunch the crashed app
    And I configure Bugsnag for "AutoDetectAnrsFalseScenario"
    Then Bugsnag confirms it has no errors to send

  Scenario: JVM exception captured with autoNotify reenabled
    When I run "UnhandledJvmAutoNotifyTrueScenario" and relaunch the crashed app
    And I configure Bugsnag for "UnhandledJvmAutoNotifyTrueScenario"
    Then I wait to receive an error
    And the error is valid for the error reporting API version "4.0" for the "Android Bugsnag Notifier" notifier
    And the error payload field "events" is an array with 1 elements
    And the exception "errorClass" equals "java.lang.RuntimeException"
    And the exception "message" equals "UnhandledJvmAutoNotifyTrueScenario"
    And the exception "type" equals "android"
    And the event "unhandled" is true
    And the event "severity" equals "error"

  Scenario: NDK signal captured with autoNotify reenabled
    When I run "UnhandledNdkAutoNotifyTrueScenario" and relaunch the crashed app
    And I configure Bugsnag for "UnhandledNdkAutoNotifyTrueScenario"
    Then I wait to receive an error
    And the error is valid for the error reporting API version "4.0" for the "Android Bugsnag Notifier" notifier
    And the error payload field "events" is an array with 1 elements
    And the exception "message" equals "Abort program"
    And the exception "type" equals "c"
    And the event "unhandled" is true
    And the event "severity" equals "error"
    And the event "severityReason.type" equals "signal"
    And the event "severityReason.unhandledOverridden" is false

# PLAT-6620
  @skip_android_8_1
  Scenario: ANR captured with autoDetectAnrs reenabled
    When I run "AutoDetectAnrsTrueScenario"
    And I wait for 2 seconds
    And I tap the screen 3 times
    And I wait for 4 seconds
    And I clear any error dialogue
    And I wait to receive an error
    Then the error is valid for the error reporting API version "4.0" for the "Android Bugsnag Notifier" notifier
    And the error payload field "events" is an array with 1 elements
    And the exception "errorClass" equals "ANR"
    And the exception "message" starts with " Input dispatching timed out"
    And the exception "type" equals "android"
    And the event "unhandled" is true
    And the event "severity" equals "error"
    And the event "severityReason.type" equals "anrError"
    And the event "severityReason.unhandledOverridden" is false
