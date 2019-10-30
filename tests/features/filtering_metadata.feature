Feature: Metadata is redacted

Scenario: Using the default metadata redact keys
    When I run "AutoRedactKeysScenario"
    Then I wait to receive a request
    And the request is valid for the error reporting API version "4.0" for the "Android Bugsnag Notifier" notifier
    And the exception "message" equals "AutoRedactKeysScenario"
    And the event "metaData.custom.foo" equals "hunter2"
    And the event "metaData.custom.password" equals "[REDACTED]"
    And the event "metaData.user.password" equals "[REDACTED]"

Scenario: Adding a custom metadata redact keys
    When I run "ManualRedactKeysScenario"
    Then I wait to receive a request
    And the request is valid for the error reporting API version "4.0" for the "Android Bugsnag Notifier" notifier
    And the exception "message" equals "ManualRedactKeysScenario"
    And the event "metaData.custom.foo" equals "[REDACTED]"
    And the event "metaData.user.foo" equals "[REDACTED]"
    And the event "metaData.custom.bar" equals "hunter2"
