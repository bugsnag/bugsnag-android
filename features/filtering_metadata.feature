Feature: Metadata is filtered

Scenario: Using the default metadata filter
    When I run "AutoFilterScenario"
    Then I wait to receive a request
#    And the request is valid for the error reporting API version "4.0" for the "Android Bugsnag Notifier" notifier
    And the exception "message" equals "AutoFilterScenario"
    And the event "metaData.custom.foo" equals "hunter2"
    And the event "metaData.custom.password" equals "[FILTERED]"
    And the event "metaData.user.password" equals "[FILTERED]"

Scenario: Adding a custom metadata filter
    When I run "ManualFilterScenario"
    Then I wait to receive a request
#    And the request is valid for the error reporting API version "4.0" for the "Android Bugsnag Notifier" notifier
    And the exception "message" equals "ManualFilterScenario"
    And the event "metaData.custom.foo" equals "[FILTERED]"
    And the event "metaData.user.foo" equals "[FILTERED]"
    And the event "metaData.custom.bar" equals "hunter2"
