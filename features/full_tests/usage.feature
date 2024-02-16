Feature: Reporting Errors with usage info

  Background:
    Given I clear all persistent data

    @skip_above_android_11
  Scenario: Report a handled exception with custom configuration and set callbacks
    When I configure the app to run in the "USAGE" state
    And I run "HandledExceptionWithUsageScenario"
    Then I wait to receive an error
    And the error is valid for the error reporting API version "4.0" for the "Android Bugsnag Notifier" notifier
    And the error payload field "events" is an array with 1 elements
    And the exception "errorClass" equals "java.lang.RuntimeException"
    And the event "exceptions.0.message" equals "HandledExceptionWithUsageScenario"
    And the error payload field "events.0.device.cpuAbi" is a non-empty array
    And the event "usage.config.maxBreadcrumbs" equals 10
    And the event "usage.config.autoTrackSessions" is false
    And the event "usage.callbacks.event_set_user" is true
    And the event "usage.callbacks.ndkOnError" equals 1
    And the event "usage.callbacks.onBreadcrumb" equals 1
    And the event "usage.callbacks.onError" equals 3
    And the event "usage.callbacks.onSession" equals 3

  Scenario: Report a handled exception with custom configuration and set callbacks, usage disabled
    When I configure the app to run in the "disable-usage" state
    And I run "HandledExceptionWithUsageScenario"
    Then I wait to receive an error
    And the error is valid for the error reporting API version "4.0" for the "Android Bugsnag Notifier" notifier
    And the error payload field "events" is an array with 1 elements
    And the exception "errorClass" equals "java.lang.RuntimeException"
    And the event "exceptions.0.message" equals "HandledExceptionWithUsageScenario"
    And the error payload field "events.0.device.cpuAbi" is a non-empty array
    And the event "usage.config.maxBreadcrumbs" is null
    And the event "usage.config.autoTrackSessions" is null
    And the event "usage.callbacks.event_set_user" is null
    And the event "usage.callbacks.ndkOnError" is null
    And the event "usage.callbacks.onBreadcrumb" is null
    And the event "usage.callbacks.onError" is null
    And the event "usage.callbacks.onSession" is null

  @skip_above_android_11
  Scenario: Report an unhandled exception with custom configuration and set callbacks
    When I configure the app to run in the "USAGE" state
    And I run "UnhandledExceptionWithUsageScenario" and relaunch the crashed app
    And I configure Bugsnag for "UnhandledExceptionWithUsageScenario"
    Then I wait to receive an error
    And the error is valid for the error reporting API version "4.0" for the "Android Bugsnag Notifier" notifier
    And the error payload field "events" is an array with 1 elements
    And the exception "errorClass" equals "java.lang.RuntimeException"
    And the event "exceptions.0.message" equals "UnhandledExceptionWithUsageScenario"
    And the error payload field "events.0.device.cpuAbi" is a non-empty array
    And the event "usage.config.maxBreadcrumbs" equals 10
    And the event "usage.config.autoTrackSessions" is false
    And the event "usage.callbacks.event_set_user" is true
    And the event "usage.callbacks.ndkOnError" equals 1
    And the event "usage.callbacks.onBreadcrumb" equals 3
    And the event "usage.callbacks.onError" equals 2
    And the event "usage.callbacks.onSession" equals 2

  Scenario: Report an unhandled exception with custom configuration and set callbacks, usage disabled
    When I configure the app to run in the "disable-usage" state
    And I run "UnhandledExceptionWithUsageScenario" and relaunch the crashed app
    And I configure Bugsnag for "UnhandledExceptionWithUsageScenario"
    Then I wait to receive an error
    And the error is valid for the error reporting API version "4.0" for the "Android Bugsnag Notifier" notifier
    And the error payload field "events" is an array with 1 elements
    And the exception "errorClass" equals "java.lang.RuntimeException"
    And the event "exceptions.0.message" equals "UnhandledExceptionWithUsageScenario"
    And the error payload field "events.0.device.cpuAbi" is a non-empty array
    And the event "usage.config.maxBreadcrumbs" is null
    And the event "usage.config.autoTrackSessions" is null
    And the event "usage.callbacks.event_set_user" is null
    And the event "usage.callbacks.ndkOnError" is null
    And the event "usage.callbacks.onBreadcrumb" is null
    And the event "usage.callbacks.onError" is null
    And the event "usage.callbacks.onSession" is null

  @skip_above_android_11
  Scenario: Report a native exception with custom configuration and set callbacks
    When I configure the app to run in the "USAGE" state
    And I run "CXXExceptionWithUsageScenario" and relaunch the crashed app
    And I configure Bugsnag for "CXXExceptionWithUsageScenario"
    Then I wait to receive an error
    And the error is valid for the error reporting API version "4.0" for the "Android Bugsnag Notifier" notifier
    And the error payload field "events" is an array with 1 elements
    And the error payload field "events.0.device.cpuAbi" is a non-empty array
    And the event "usage.config.maxBreadcrumbs" equals 10
    And the event "usage.config.autoTrackSessions" is false
    And the event "usage.config.discardClassesCount" equals 3
    And the event "usage.config.maxPersistedSessions" equals 1000
    And the event "usage.callbacks.onBreadcrumb" equals 1
    And the event "usage.callbacks.onError" equals 2
    And the event "usage.callbacks.onSession" equals 4

  Scenario: Report a native exception with custom configuration and set callbacks, usage disabled
    When I configure the app to run in the "disable-usage" state
    And I run "CXXExceptionWithUsageScenario" and relaunch the crashed app
    And I configure Bugsnag for "CXXExceptionWithUsageScenario"
    Then I wait to receive an error
    And the error is valid for the error reporting API version "4.0" for the "Android Bugsnag Notifier" notifier
    And the error payload field "events" is an array with 1 elements
    And the error payload field "events.0.device.cpuAbi" is a non-empty array
    And the event "usage.config.maxBreadcrumbs" is null
    And the event "usage.config.autoTrackSessions" is null
    And the event "usage.config.discardClassesCount" is null
    And the event "usage.config.maxPersistedSessions" is null
    And the event "usage.callbacks.onBreadcrumb" is null
    And the event "usage.callbacks.onError" is null
    And the event "usage.callbacks.onSession" is null

  @skip_above_android_11
  Scenario: Report a native exception with custom configuration and set callbacks
    When I configure the app to run in the "USAGE" state
    And I run "CXXSigsegvWithUsageScenario" and relaunch the crashed app
    And I configure Bugsnag for "CXXSigsegvWithUsageScenario"
    Then I wait to receive an error
    And the error is valid for the error reporting API version "4.0" for the "Android Bugsnag Notifier" notifier
    And the error payload field "events" is an array with 1 elements
    And the exception "errorClass" equals "SIGSEGV"
    And the event "exceptions.0.message" equals "Segmentation violation (invalid memory reference)"
    And the error payload field "events.0.device.cpuAbi" is a non-empty array
    And the event "app.binaryArch" equals "something weird"
    And the event "usage.config.maxBreadcrumbs" equals 10
    And the event "usage.config.autoTrackSessions" is false
    And the event "usage.callbacks.ndkOnError" equals 1
    And the event "usage.callbacks.onBreadcrumb" equals 1
    And the event "usage.callbacks.onError" equals 2
    And the event "usage.callbacks.onSession" equals 4
    And the event "usage.callbacks.event_set_user" is true
    And the event "usage.callbacks.app_set_binary_arch" is true
    And the event "usage.callbacks.device_get_model" is true
    And the event "usage.callbacks.event_get_severity" is true

  Scenario: Report a native exception with custom configuration and set callbacks, usage disabled
    When I configure the app to run in the "disable-usage" state
    And I run "CXXSigsegvWithUsageScenario" and relaunch the crashed app
    And I configure Bugsnag for "CXXSigsegvWithUsageScenario"
    Then I wait to receive an error
    And the error is valid for the error reporting API version "4.0" for the "Android Bugsnag Notifier" notifier
    And the error payload field "events" is an array with 1 elements
    And the exception "errorClass" equals "SIGSEGV"
    And the event "exceptions.0.message" equals "Segmentation violation (invalid memory reference)"
    And the error payload field "events.0.device.cpuAbi" is a non-empty array
    And the event "app.binaryArch" equals "something weird"
    And the event "usage.config.maxBreadcrumbs" is null
    And the event "usage.config.autoTrackSessions" is null
    And the event "usage.callbacks.ndkOnError" is null
    And the event "usage.callbacks.onBreadcrumb" is null
    And the event "usage.callbacks.onError" is null
    And the event "usage.callbacks.onSession" is null
    And the event "usage.callbacks.event_set_user" is null
    And the event "usage.callbacks.app_set_binary_arch" is null
    And the event "usage.callbacks.device_get_model" is null
    And the event "usage.callbacks.event_get_severity" is null
