Feature: Android support

Scenario: Notify release stage null
    When I run "NullNotifyReleaseStageScenario" with the defaults
    Then I should receive a request
    And the request is a valid for the error reporting API
    And the exception "errorClass" equals "java.lang.RuntimeException"
    And the exception "message" equals "NullNotifyReleaseStageScenario"
