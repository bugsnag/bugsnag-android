Feature: In foreground field populates correctly

Scenario: Test handled exception after delay
    When I run "HandledExceptionScenario"
    Then I should receive a request
    And the request is a valid for the error reporting API
    And the event "app.inForeground" is true

#    Duration in foreground should be a non-zero integer
    And the payload field "events.0.app.durationInForeground" is greater than 0

Scenario: Test handled exception in background
    When I run "InForegroundScenario" and press the home button
    And I press the home button
    Then I should receive a request
    And the request is a valid for the error reporting API
    And the event "app.inForeground" is false
    And the payload field "events.0.app.durationInForeground" equals 0
