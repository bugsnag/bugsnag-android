Feature: All errors are flushed if a startup crash is persisted

Scenario: Crash on startup is delivered synchronously
    When I configure the app to run in the "CrashOfflineAtStartup" state
    And I run "StartupCrashFlushScenario"
    And I configure the app to run in the "non-crashy" state
    And I relaunch the app
    Then I should receive 1 request
