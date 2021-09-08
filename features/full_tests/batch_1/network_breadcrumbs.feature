Feature: Capturing network breadcrumbs

Scenario: Breadcrumbs are captured for OkHttp network requests
    When I run "NetworkBreadcrumbScenario"
    And I wait to receive an error
    Then the error is valid for the error reporting API version "4.0" for the "Android Bugsnag Notifier" notifier
    And the error payload field "events" is an array with 1 elements
    And the exception "errorClass" equals "java.lang.RuntimeException"
    And the exception "message" equals "NetworkBreadcrumbScenario"

    # TODO validate that an actual network breadcrumb was captured
    And the event has 1 breadcrumbs
    And the event "breadcrumbs.0.timestamp" is a timestamp
    And the event "breadcrumbs.0.name" equals "My request breadcrumb"
    And the event "breadcrumbs.0.type" equals "request"
