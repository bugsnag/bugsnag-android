Feature: Reporting with other exception handlers installed

  Background:
    Given I clear all persistent data

  Scenario: Unhandled exception with max threads set
    When I run "UnhandledExceptionMaxThreadsScenario" and relaunch the crashed app
    And I configure Bugsnag for "UnhandledExceptionMaxThreadsScenario"
    And I wait to receive an error
    Then the error is valid for the error reporting API version "4.0" for the "Android Bugsnag Notifier" notifier
    And the error payload field "events" is an array with 1 elements
    And the exception "errorClass" equals "java.lang.RuntimeException"
    And the exception "message" equals "UnhandledExceptionMaxThreadsScenario"
    And the error payload field "events.0.threads" is an array with 3 elements
    And the error payload field "events.0.threads.2.id" equals ""
    And the error payload field "events.0.threads.2.name" ends with "threads omitted as the maxReportedThreads limit (2) was exceeded]"

  Scenario: Handled exception with max threads set
    When I run "HandledExceptionMaxThreadsScenario"
    And I wait to receive an error
    Then the error is valid for the error reporting API version "4.0" for the "Android Bugsnag Notifier" notifier
    And the error payload field "events" is an array with 1 elements
    And the exception "errorClass" equals "java.lang.RuntimeException"
    And the exception "message" equals "HandledExceptionMaxThreadsScenario"
    And the error payload field "events.0.threads" is an array with 4 elements
    And the error payload field "events.0.threads.3.id" equals ""
    And the error payload field "events.0.threads.3.name" ends with "threads omitted as the maxReportedThreads limit (3) was exceeded]"
