Feature: Reporting Errors with usage info

  Background:
    Given I clear all persistent data

  Scenario: Report a handled exception with custom configuration
    When I run "HandledExceptionWithUsageScenario"
    Then I wait to receive an error
    And the error is valid for the error reporting API version "4.0" for the "Android Bugsnag Notifier" notifier
    And the error payload field "events" is an array with 1 elements
    And the exception "errorClass" equals "java.lang.RuntimeException"
    And the event "exceptions.0.message" equals "HandledExceptionWithUsageScenario"
    And the error payload field "events.0.device.cpuAbi" is a non-empty array
    And the event "config.maxBreadcrumbs" equals 10
    And the event "config.autoTrackSessions" is false

  Scenario: Report an unhandled exception with custom configuration
    When I run "UnhandledExceptionWithUsageScenario" and relaunch the crashed app
    And I configure Bugsnag for "UnhandledExceptionWithUsageScenario"
    Then I wait to receive an error
    And the error is valid for the error reporting API version "4.0" for the "Android Bugsnag Notifier" notifier
    And the error payload field "events" is an array with 1 elements
    And the exception "errorClass" equals "java.lang.RuntimeException"
    And the event "exceptions.0.message" equals "UnhandledExceptionWithUsageScenario"
    And the error payload field "events.0.device.cpuAbi" is a non-empty array
    And the event "config.maxBreadcrumbs" equals 10
    And the event "config.autoTrackSessions" is false

  Scenario: Report a native exception with custom configuration
    When I run "CXXExceptionWithUsageScenario" and relaunch the crashed app
    And I configure Bugsnag for "CXXExceptionWithUsageScenario"
    Then I wait to receive an error
    And the error is valid for the error reporting API version "4.0" for the "Android Bugsnag Notifier" notifier
    And the error payload field "events" is an array with 1 elements
    And the exception "errorClass" equals "SIGABRT"
    And the event "exceptions.0.message" equals "Abort program"
    And the error payload field "events.0.device.cpuAbi" is a non-empty array
    And the event "config.maxBreadcrumbs" equals 10
    And the event "config.autoTrackSessions" is false

  Scenario: Report a native exception with custom configuration
    When I run "CXXSigsegvWithUsageScenario" and relaunch the crashed app
    And I configure Bugsnag for "CXXSigsegvWithUsageScenario"
    Then I wait to receive an error
    And the error is valid for the error reporting API version "4.0" for the "Android Bugsnag Notifier" notifier
    And the error payload field "events" is an array with 1 elements
    And the exception "errorClass" equals "SIGSEGV"
    And the event "exceptions.0.message" equals "Segmentation violation (invalid memory reference)"
    And the error payload field "events.0.device.cpuAbi" is a non-empty array
    And the event "config.maxBreadcrumbs" equals 10
    And the event "config.autoTrackSessions" is false
