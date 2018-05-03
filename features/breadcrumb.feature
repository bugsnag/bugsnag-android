Feature: Reporting Breadcrumbs

Scenario: Manually added breadcrumbs are sent in report
    When I run "BreadcrumbScenario" with the defaults
    Then I should receive a request
    And the request is a valid for the error reporting API
    And the exception "message" equals "BreadcrumbScenario"
    And the event "breadcrumbs" is not null

    And the event "breadcrumbs.1.timestamp" is not null
    And the event "breadcrumbs.1.name" equals "Another Breadcrumb"
    And the event "breadcrumbs.1.type" equals "user"
    And the event "breadcrumbs.1.metaData.Foo" equals "Bar"

    And the event "breadcrumbs.0.timestamp" is not null
    And the event "breadcrumbs.0.name" equals "manual"
    And the event "breadcrumbs.0.type" equals "manual"
    And the event "breadcrumbs.0.metaData" is not null
    And the event "breadcrumbs.0.metaData.message" equals "Hello Breadcrumb!"
