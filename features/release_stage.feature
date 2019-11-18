Feature: Reporting exceptions with release stages

Scenario: Exception not reported when outside release stage
    When I run "OutsideReleaseStageScenario"
    Then I should receive no requests

Scenario: Exception not reported when release stage null
    When I run "NullReleaseStageScenario"
    Then I should receive no requests

Scenario: Exception reported when release stages empty
    When I run "EmptyEnabledReleaseStageScenario"
    Then I should receive a request
    And the request is a valid for the error reporting API
    And the exception "errorClass" equals "java.lang.RuntimeException"
    And the exception "message" equals "EmptyEnabledReleaseStageScenario"

Scenario: Exception reported when inside release stage
    When I run "InsideReleaseStageScenario"
    Then I should receive a request
    And the request is a valid for the error reporting API
    And the "Bugsnag-API-Key" header equals "a35a2a72bd230ac0aa0f52715bbdc6aa"
    And the payload field "notifier.name" equals "Android Bugsnag Notifier"
    And the payload field "events" is an array with 1 element
    And the exception "errorClass" equals "java.lang.RuntimeException"
    And the exception "message" equals "InsideReleaseStageScenario"

Scenario: Exception reported when inside Notify release stage array
    When I run "ArrayEnabledReleaseStageScenario"
    Then I should receive a request
    And the request is a valid for the error reporting API
    And the exception "errorClass" equals "java.lang.RuntimeException"
    And the exception "message" equals "ArrayEnabledReleaseStageScenario"
