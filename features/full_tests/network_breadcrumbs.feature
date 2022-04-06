Feature: Capturing network breadcrumbs

  Background:
    Given I clear all persistent data

  Scenario: Breadcrumbs are captured for OkHttp network requests
    When I run "NetworkBreadcrumbScenario"
    And I wait to receive an error
    Then the error is valid for the error reporting API version "4.0" for the "Android Bugsnag Notifier" notifier
    And the error payload field "events" is an array with 1 elements
    And the exception "errorClass" equals "java.lang.RuntimeException"
    And the exception "message" equals "NetworkBreadcrumbScenario"

    And the event has 1 breadcrumbs
    And the event "breadcrumbs.0.timestamp" is a timestamp
    And the event "breadcrumbs.0.name" equals "OkHttp call succeeded"
    And the event "breadcrumbs.0.type" equals "request"
    And the event "breadcrumbs.0.metaData.method" equals "GET"
    And the event "breadcrumbs.0.metaData.url" equals "https://google.com/"
    And the error payload field "events.0.breadcrumbs.0.metaData.duration" is a number
    And the error payload field "events.0.exceptions.0.stacktrace.0.type" is null
    And the event "breadcrumbs.0.metaData.urlParams.test" equals "true"
    And the error payload field "events.0.breadcrumbs.0.metaData.requestContentLength" is a number
    And the error payload field "events.0.breadcrumbs.0.metaData.responseContentLength" is a number
    And the error payload field "events.0.breadcrumbs.0.metaData.status" is a number
