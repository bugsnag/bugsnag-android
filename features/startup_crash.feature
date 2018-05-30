Feature: All errors are flushed if a startup crash is persisted

Scenario: 1 startup crash and 1 regular crash persisted
    When I configure the app to run in the "CrashOfflineWithDelay" state
    And I run "StartupCrashFlushScenario" with the defaults
    And I wait for 10 seconds

    And I configure the app to run in the "CrashOfflineAtStartup" state
    And I relaunch the app

    And I configure the app to run in the "No crash" state
    And I relaunch the app
    And I wait for 5 seconds
    Then I should receive 2 requests
