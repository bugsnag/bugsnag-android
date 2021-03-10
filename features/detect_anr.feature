Feature: Reporting App Not Responding events

Scenario: Sleeping the main thread with pending touch events when detectAnrs = true
    When I run "AppNotRespondingScenario"
    And I wait for 2 seconds
    And I tap the screen 3 times
    And I wait for 4 seconds
    And I clear any error dialogue
    Then I wait to receive a request
    And the request is valid for the error reporting API version "4.0" for the "Android Bugsnag Notifier" notifier
    And the exception "errorClass" equals "ANR"
    And the exception "message" starts with " Input dispatching timed out"

Scenario: Sleeping the main thread with pending touch events when detectAnrs = true and detectNdkCrashes = false
    When I run "AppNotRespondingDisabledNdkScenario"
    And I wait for 2 seconds
    And I tap the screen 3 times
    And I wait for 4 seconds
    And I clear any error dialogue
    Then I wait to receive a request
    And the request is valid for the error reporting API version "4.0" for the "Android Bugsnag Notifier" notifier
    And the exception "errorClass" equals "ANR"
    And the exception "message" starts with " Input dispatching timed out"

Scenario: Sleeping the main thread with pending touch events
    When I run "AppNotRespondingDisabledScenario"
    And I wait for 2 seconds
    And I tap the screen 3 times
    And I wait for 4 seconds
    And I clear any error dialogue
    Then I should receive no requests

Scenario: Sleeping the main thread with pending touch events after disabling ANR reporting
    When I run "AppNotRespondingLaterDisabledScenario"
    And I wait for 2 seconds
    And I tap the screen 3 times
    And I wait for 4 seconds
    And I clear any error dialogue
    Then I should receive no requests

Scenario: Sleeping the main thread with pending touch events after the release stage settings change to disable reporting
    When I run "AppNotRespondingOutsideReleaseStagesScenario"
    And I wait for 2 seconds
    And I tap the screen 3 times
    And I wait for 4 seconds
    And I clear any error dialogue
    Then I should receive no requests
