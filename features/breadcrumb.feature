Feature: Reporting Breadcrumbs

Scenario: Manually added breadcrumbs are sent in report
    When I run "BreadcrumbScenario"
    And I clear any error dialogue
    And I wait to receive a request
    Then the request is valid for the error reporting API version "4.0" for the "Android Bugsnag Notifier" notifier
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
