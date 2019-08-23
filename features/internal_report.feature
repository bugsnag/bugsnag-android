Feature: Sending internal error reports

Scenario: Send a report about an error triggered within the notifier
    When I run "InternalReportScenario"
    And I set environment variable "EVENT_TYPE" to "EmptyReportScenario"
    And I relaunch the app
    Then I should receive 1 request
    And the "Bugsnag-Internal-Error" header equals "true"
    And the payload field "apiKey" is null
    And the event "context" equals "Crash Report Deserialization"
    And the event "session" is null
    And the event "breadcrumbs" is null
    And the event "app.type" equals "android"
    And the event "device.osName" equals "android"
