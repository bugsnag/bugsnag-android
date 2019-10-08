Feature: Cached Session Reports

Scenario: If an empty file is in the cache directory then zero requests should be made
    When I run "EmptySessionScenario"
    And I configure the app to run in the "non-crashy" state
    And I relaunch the app
    Then I should receive 0 requests

Scenario: Sending internal error reports on API <26
    When I run "PartialSessionScenario"
    And I configure the app to run in the "non-crashy" state
    And I relaunch the app
    Then I should receive 0 requests

Scenario: If a file in the cache directory is deleted before a request completes, zero further requests should be made
    When I run "DeletedSessionScenario"
    And I configure the app to run in the "non-crashy" state
    And I relaunch the app
    Then I should receive 0 requests
