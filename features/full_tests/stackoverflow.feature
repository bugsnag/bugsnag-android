Feature: Reporting Stack overflow

  Background:
    Given I clear all persistent data

  Scenario: Stack Overflow sends
    When I run "StackOverflowScenario" and relaunch the crashed app
    And I configure Bugsnag for "StackOverflowScenario"
    And I wait to receive an error
    And the error is valid for the error reporting API version "4.0" for the "Android Bugsnag Notifier" notifier
    And the error payload field "events" is an array with 1 elements
    And the exception "errorClass" equals "java.lang.StackOverflowError"
    And the "method" of stack frame 0 equals "com.bugsnag.android.mazerunner.scenarios.StackOverflowScenario.calculateValue"
    And the "method" of stack frame 1 equals "com.bugsnag.android.mazerunner.scenarios.StackOverflowScenario.calculateValue"
    And the "method" of stack frame 2 equals "com.bugsnag.android.mazerunner.scenarios.StackOverflowScenario.calculateValue"
    And the "method" of stack frame 3 equals "com.bugsnag.android.mazerunner.scenarios.StackOverflowScenario.calculateValue"
