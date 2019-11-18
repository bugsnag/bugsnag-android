Feature: Reporting Error Context

Scenario: Context automatically set as most recent Activity name
    When I run "AutoContextScenario"
    Then I wait to receive a request
    And the request is valid for the error reporting API version "4.0" for the "Android Bugsnag Notifier" notifier
    And the exception "message" equals "AutoContextScenario"
    And the event "context" equals "SecondActivity"

Scenario: Context manually set
    When I run "ManualContextScenario"
    Then I wait to receive a request
    And the request is valid for the error reporting API version "4.0" for the "Android Bugsnag Notifier" notifier
    And the exception "message" equals "ManualContextScenario"
    And the event "context" equals "FooContext"
