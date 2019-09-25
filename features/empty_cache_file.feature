Feature: Empty cache files are not reported

Scenario: A zero-length file does not trigger an error report
    When I run "EmptyCacheFileScenario"
    When I configure the app to run in the "online" state
    And I relaunch the app
    Then I should receive 0 requests
