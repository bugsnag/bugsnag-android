Feature: Reporting Breadcrumbs

  Background:
    Given I clear all persistent data

  Scenario: Manually added breadcrumbs are sent in report
    When I run "BreadcrumbScenario"
    And I wait to receive an error
    Then the error is valid for the error reporting API version "4.0" for the "Android Bugsnag Notifier" notifier
    And the exception "message" equals "BreadcrumbScenario"
    And the event "breadcrumbs" is not null

    And the event "breadcrumbs.1.timestamp" is not null
    And the event "breadcrumbs.1.name" equals "Another Breadcrumb"
    And the event "breadcrumbs.1.type" equals "user"
    And the event "breadcrumbs.1.metaData.Foo" equals "Bar"
    And the event "breadcrumbs.1.metaData.password" equals "[REDACTED]"

    And the event "breadcrumbs.0.timestamp" is not null
    And the event "breadcrumbs.0.name" equals "Hello Breadcrumb!"
    And the event "breadcrumbs.0.type" equals "manual"

  Scenario: Manually added breadcrumbs are sent in report when auto breadcrumbs are disabled
    When I run "BreadcrumbDisabledScenario"
    And I wait to receive an error
    Then the error is valid for the error reporting API version "4.0" for the "Android Bugsnag Notifier" notifier
    And the event has 1 breadcrumbs

  Scenario: An automatic breadcrumb is sent in report when the appropriate type is enabled
    When I run "BreadcrumbAutoScenario"
    And I wait to receive an error
    Then the error is valid for the error reporting API version "4.0" for the "Android Bugsnag Notifier" notifier
    And the event has a "state" breadcrumb named "Bugsnag loaded"

  Scenario: Error Breadcrumbs appear in subsequent events
    When I run "ErrorBreadcrumbsScenario" and relaunch the crashed app
    And I configure Bugsnag for "ErrorBreadcrumbsScenario"
    Then I wait to receive 2 errors
    And I sort the errors by the payload field "events.0.exceptions.0.errorClass"

    Then the exception "errorClass" equals "java.lang.NullPointerException"
    And the exception "message" equals "something broke"
    And the event "unhandled" is true
    And the event has 1 breadcrumbs
    And the event "breadcrumbs.0.timestamp" is not null
    And the event "breadcrumbs.0.name" equals "java.lang.RuntimeException"
    And the event "breadcrumbs.0.type" equals "error"
    And the event "breadcrumbs.0.metaData.errorClass" equals "java.lang.RuntimeException"
    And the event "breadcrumbs.0.metaData.message" equals "first error"
    And the event "breadcrumbs.0.metaData.unhandled" equals "false"
    And the event "breadcrumbs.0.metaData.severity" equals "WARNING"

    Then I discard the oldest error
    And the exception "errorClass" equals "java.lang.RuntimeException"
    And the exception "message" equals "first error"
    And the event "unhandled" is false
    And the event has 0 breadcrumbs
