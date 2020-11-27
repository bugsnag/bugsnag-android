Feature: Cached Error Reports

Scenario: If an empty file is in the cache directory then zero requests should be made
    When I run "EmptyReportScenario" and relaunch the app
    And I configure the app to run in the "non-crashy" state
    And I run "EmptyReportScenario"
    Then I should receive no requests

@skip_above_android_7
Scenario: Sending internal error reports on API <26
    When I run "InternalReportScenario" and relaunch the app
    And I configure the app to run in the "non-crashy" state
    And I run "InternalReportScenario"
    Then I should receive no requests

@skip_below_android_8
Scenario: Sending internal error reports on API >=26
    When I run "InternalReportScenario" and relaunch the app
    And I configure the app to run in the "non-crashy" state
    And I run "InternalReportScenario"
    Then I should receive no requests

@skip_below_android_8
Scenario: Sending internal error reports with cache tombstone + groups enabled
    And I configure the app to run in the "tombstone" state
    When I run "InternalReportScenario" and relaunch the app
    And I configure the app to run in the "non-crashy" state
    And I run "InternalReportScenario"
    Then I should receive no requests

# skip until PLAT-5488 is addressed
@skip_below_android_5
Scenario: If a file in the cache directory is deleted before a request completes, zero further requests should be made
    When I run "DeletedReportScenario" and relaunch the app
    And I configure the app to run in the "non-crashy" state
    And I run "DeletedReportScenario"
    Then I should receive no requests
