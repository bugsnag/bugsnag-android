Feature: Reporting exceptions with release stages

Scenario: Exception not reported when outside release stage
    When I run "OutsideReleaseStageScenario"
    And I wait for 2 seconds
    Then I should receive no requests

Scenario: Exception not reported when release stage null
    When I run "NullReleaseStageScenario"
    Then I should receive no requests

Scenario: Exception reported when release stages empty
    When I run "EmptyEnabledReleaseStageScenario"
    Then I should receive no requests

Scenario: Exception reported when inside release stage
    When I run "InsideReleaseStageScenario"
    Then I wait to receive a request
    And the request is valid for the error reporting API version "4.0" for the "Android Bugsnag Notifier" notifier
    And the payload field "notifier.name" equals "Android Bugsnag Notifier"
    And the payload field "events" is an array with 1 elements
    And the exception "errorClass" equals "java.lang.RuntimeException"
    And the exception "message" equals "InsideReleaseStageScenario"

Scenario: Exception reported when inside Notify release stage array
    When I run "ArrayEnabledReleaseStageScenario"
    Then I wait to receive a request
    And the request is valid for the error reporting API version "4.0" for the "Android Bugsnag Notifier" notifier
    And the exception "errorClass" equals "java.lang.RuntimeException"
    And the exception "message" equals "ArrayEnabledReleaseStageScenario"
