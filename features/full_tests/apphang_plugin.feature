Feature: AppHang Plugin

  Background:
    Given I clear all persistent data

  Scenario: AppHangPlugin Reports AppHang errors
    When I run "AppHangPluginScenario"
    Then I wait to receive an error
    And the error is valid for the error reporting API version "4.0" for the "Android Bugsnag Notifier" notifier
    And the exception "errorClass" equals "AppHang"


  Scenario: StackSampling reports cause of AppHang
    When I run "SampledAppHangScenario"
    Then I wait to receive an error
    And the error is valid for the error reporting API version "4.0" for the "Android Bugsnag Notifier" notifier
    And the error payload field "events.0.exceptions" is an array with 2 elements
    And the error payload field "events.0.exceptions.1.stacktrace" is a non-empty array
    And the error payload field "events.0.exceptions.1.stacktrace.0.method" equals "com.bugsnag.android.mazerunner.scenarios.SampledAppHangScenario.verySlowFunction"
    And the error payload field "events.0.exceptions.0.stacktrace.0.method" starts with "com.bugsnag.android.mazerunner.scenarios.SampledAppHangScenario.startScenario"
