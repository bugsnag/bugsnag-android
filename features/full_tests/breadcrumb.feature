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
    And I wait to receive a request
    Then the request is valid for the error reporting API version "4.0" for the "Android Bugsnag Notifier" notifier
    And the event has 1 breadcrumbs

Scenario: An automatic breadcrumb is sent in report when the appropriate type is enabled
    When I run "BreadcrumbAutoScenario"
    And I wait to receive a request
    Then the request is valid for the error reporting API version "4.0" for the "Android Bugsnag Notifier" notifier
    And the event has a "state" breadcrumb with the message "Bugsnag loaded"