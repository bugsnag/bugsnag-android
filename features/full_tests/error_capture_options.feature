Feature: ErrorCaptureOptions

  Background:
    Given I clear all persistent data

  Scenario: Capture only stacktrace
    When I configure the app to run in the "stacktrace" state
    And I run "ErrorOptionsScenario"
    Then I wait to receive an error
    And the error is valid for the error reporting API version "4.0" for the "Android Bugsnag Notifier" notifier
        # Stacktrace validation
    And the error payload field "events.0.exceptions.0.stacktrace" is a non-empty array
    And the event "exceptions.0.stacktrace.0.method" ends with "com.bugsnag.android.mazerunner.scenarios.Scenario.generateException"
    And the exception "stacktrace.0.file" equals "SourceFile"

    And the event "user.id" is null
    And the event "user.name" is null
    And the error payload field "events.0.breadcrumbs" is an array with 0 elements
    And the error payload field "events.0.featureFlags" is an array with 0 elements
    And the error payload field "events.0.threads" is an array with 0 elements

  Scenario: Capture only user
    When I configure the app to run in the "user" state
    And I run "ErrorOptionsScenario"
    Then I wait to receive an error
    And the error is valid for the error reporting API version "4.0" for the "Android Bugsnag Notifier" notifier

    And the event "user.id" equals "123"
    And the event "user.email" equals "jane@doe.com"
    And the event "user.name" equals "Jane Doe"

    And the error payload field "events.0.breadcrumbs" is an array with 0 elements
    And the error payload field "events.0.featureFlags" is an array with 0 elements
    And the error payload field "events.0.threads" is an array with 0 elements

  Scenario: Capture only breadcrumbs
    When I configure the app to run in the "breadcrumbs" state
    And I run "ErrorOptionsScenario"
    Then I wait to receive an error
    And the error is valid for the error reporting API version "4.0" for the "Android Bugsnag Notifier" notifier

    And the event has 2 breadcrumbs
    And the event "breadcrumbs.1.name" equals "Test breadcrumb"

    And the event "user.id" is null
    And the event "user.name" is null
    And the error payload field "events.0.featureFlags" is an array with 0 elements
    And the error payload field "events.0.threads" is an array with 0 elements

  Scenario: Capture only feature flags
    When I configure the app to run in the "featureFlags" state
    And I run "ErrorOptionsScenario"
    Then I wait to receive an error
    And the error is valid for the error reporting API version "4.0" for the "Android Bugsnag Notifier" notifier
    And the event "user.id" is null
    And the event "user.name" is null
    And the error payload field "events.0.breadcrumbs" is an array with 0 elements
    And the error payload field "events.0.threads" is an array with 0 elements

    And the error payload field "events.0.featureFlags" is an array with 2 elements
    And event 0 contains the feature flag "testFeatureFlag" with variant "variantA"
    And event 0 contains the feature flag "featureFlag2" with no variant

  Scenario: Capture selected metadata and stacktrace
    When I configure the app to run in the "stacktrace custom2" state
    And I run "ErrorOptionsScenario"
    Then I wait to receive an error
    And the error is valid for the error reporting API version "4.0" for the "Android Bugsnag Notifier" notifier
        # Stacktrace validation
    And the error payload field "events.0.exceptions.0.stacktrace" is a non-empty array
    And the event "exceptions.0.stacktrace.0.method" ends with "com.bugsnag.android.mazerunner.scenarios.Scenario.generateException"
    And the exception "stacktrace.0.file" equals "SourceFile"

    And the event "user.id" is null
    And the event "user.name" is null
    And the error payload field "events.0.breadcrumbs" is an array with 0 elements
    And the error payload field "events.0.featureFlags" is an array with 0 elements
    And the event "metaData.custom2.testKey2" equals "value"
