Feature: Tombstoned cache files are not reported

Scenario: A zero-length file is not reported when cache tombstone behavior is set
    When I run "CacheTombstoneScenario"
    When I configure the app to run in the "online" state
    And I relaunch the app
    Then I should receive 0 requests
