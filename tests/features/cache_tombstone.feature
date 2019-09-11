Feature: Tombstoned cache files are not reported

@skip_below_android_9
Scenario: A zero-length file is not reported when cache tombstone behavior is set
    When I run "CacheTombstoneScenario" and relaunch the app
    And I configure the app to run in the "online" state
    And I run "CacheTombstoneScenario"
    Then I should receive 0 requests
