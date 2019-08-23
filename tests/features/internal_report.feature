Feature: Sending internal error reports

Scenario: Sending internal error reports
    When I run "InternalReportScenario" and relaunch the app
    And I configure Bugsnag for "EmptyReportScenario"
    And I wait to receive a request
    And the "Bugsnag-Internal-Error" header equals "true"
    And the payload field "apiKey" is null
    And the event "context" equals "Crash Report Deserialization"
    And the event "session" is null
    And the event "breadcrumbs" is null
    And the event "app.type" equals "android"
    And the event "device.osName" equals "android"
