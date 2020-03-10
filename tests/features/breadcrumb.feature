Feature: Reporting Breadcrumbs

Scenario: Manually added breadcrumbs are sent in report
    When I run "BreadcrumbScenario"
    And I wait to receive a request
    Then the request is valid for the error reporting API version "4.0" for the "Android Bugsnag Notifier" notifier
    And the exception "message" equals "BreadcrumbScenario"
    And the event "breadcrumbs" is not null

    And the event "breadcrumbs.1.timestamp" is not null
    And the event "breadcrumbs.1.name" equals "Another Breadcrumb"
    And the event "breadcrumbs.1.type" equals "user"
    And the event "breadcrumbs.1.metaData.Foo" equals "Bar"

    And the event "breadcrumbs.0.timestamp" is not null
    And the event "breadcrumbs.0.name" equals "Hello Breadcrumb!"
    And the event "breadcrumbs.0.type" equals "manual"

Scenario: Manually added breadcrumbs are sent in report when auto breadcrumbs are disabled
    When I run "BreadcrumbDisabledScenario"
    Then I should receive a request
    And the request is a valid for the error reporting API
    And the event has 1 breadcrumbs

Scenario: Manually added breadcrumbs are sent in report when auto breadcrumbs are disabled
    When I run "BreadcrumbAutoScenario"
    And I relauch the app
    Then I should receive a request
    And the request is a valid for the error reporting API
    And the event has a "STATE" breadcrumb with message "Bugsnag loaded"
