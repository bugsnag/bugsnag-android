Feature: In foreground field populates correctly

Scenario: Test handled exception after delay
    When I run "InForegroundScenario"
    Then I should receive a request
    And the request is a valid for the error reporting API
    And the event "app.inForeground" is true

#    Duration in foreground should be a non-zero integer
    And the payload field "events.0.app.durationInForeground" is greater than 0
