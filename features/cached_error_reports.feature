Feature: Cached Error Reports

Scenario: If an empty file is in the cache directory then zero requests should be made
    When I run "EmptyReportScenario"
    And I configure the app to run in the "non-crashy" state
    And I relaunch the app
    Then I should receive 0 requests

@skip_above_android_7
Scenario: Sending internal error reports on API <26
    When I run "InternalReportScenario"
    And I configure the app to run in the "non-crashy" state
    And I relaunch the app
    Then I should receive 0 requests

@skip_below_android_8
Scenario: Sending internal error reports on API >=26
    When I run "InternalReportScenario"
    And I configure the app to run in the "non-crashy" state
    And I relaunch the app
    Then I should receive 0 requests

@skip_below_android_8
Scenario: Sending internal error reports with cache tombstone + groups enabled
    When I configure the app to run in the "tombstone" state
    And I run "InternalReportScenario"
    And I configure the app to run in the "non-crashy" state
    And I relaunch the app
    Then I should receive 0 requests

Scenario: If a file in the cache directory is deleted before a request completes, zero further requests should be made
    When I run "DeletedReportScenario"
    And I configure the app to run in the "non-crashy" state
    And I relaunch the app
    Then I should receive 0 requests
