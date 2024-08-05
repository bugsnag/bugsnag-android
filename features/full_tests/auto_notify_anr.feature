Feature: Switching automatic error detection on/off for Unity ANR

  Background:
    Given I clear all persistent data

  @anr
  @skip_samsung
  @skip_android_10
  Scenario: ANR not captured with autoDetectAnrs=false
    When I run "AutoDetectAnrsFalseScenario"
    And I wait for 2 seconds
    And I tap the screen 3 times
    And I wait for 10 seconds
    And I close and relaunch the app after an ANR
    And I configure Bugsnag for "AutoDetectAnrsFalseScenario"
    Then Bugsnag confirms it has no errors to send
    And I wait for 10 seconds
    #  Wait extra 10 seconds in the end, so appium will have enough time to terminated the previous anr session

  @anr
  @skip_samsung
  @skip_android_10
  Scenario: ANR captured with autoDetectAnrs reenabled
    When I clear any error dialogue
    And I run "AutoDetectAnrsTrueScenario"
    And I wait for 2 seconds
    And I tap the screen 3 times
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
