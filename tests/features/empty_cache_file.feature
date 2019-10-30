Feature: Empty cache files are not reported

Scenario: A zero-length file does not trigger an error report
    When I run "EmptyCacheFileScenario" and relaunch the app
    And I configure the app to run in the "online" state
    And I run "EmptyCacheFileScenario"
    Then I should receive no requests
