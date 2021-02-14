Feature: Cached Session Reports

Scenario: If an empty file is in the cache directory then zero requests should be made
    When I run "EmptySessionScenario" and relaunch the app
    And I run "EmptySessionScenario"
    Then I should receive no requests

# TODO: Skip pending PLAT-5488
@skip
Scenario: Sending internal error reports on API <26
    When I run "PartialSessionScenario" and relaunch the app
    And I run "PartialSessionScenario"
    Then I should receive no requests

# TODO: Skip pending PLAT-5488
@skip
Scenario: If a file in the cache directory is deleted before a request completes, zero further requests should be made
    When I run "DeletedSessionScenario" and relaunch the app
    And I run "DeletedSessionScenario"
    Then I should receive no requests
