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
    Then I should receive 1 request
    And the "Bugsnag-Internal-Error" header equals "true"
    And the payload field "apiKey" is null
    And the event "context" equals "Crash Report Deserialization"
    And the event "session" is null
    And the event "breadcrumbs" is null
    And the event "app.type" equals "android"
    And the event "device.osName" equals "android"
    And the event "metaData.BugsnagDiagnostics.filename" is not null
    And the event "metaData.BugsnagDiagnostics.notifierName" equals "Android Bugsnag Notifier"
    And the event "metaData.BugsnagDiagnostics.apiKey" equals "a35a2a72bd230ac0aa0f52715bbdc6aa"
    And the event "metaData.BugsnagDiagnostics.packageName" equals "com.bugsnag.android.mazerunner"
    And the event "metaData.BugsnagDiagnostics.notifierVersion" is not null
    And the event "metaData.BugsnagDiagnostics.fileLength" equals 4
    And the event "metaData.BugsnagDiagnostics.cacheGroup" is null
    And the event "metaData.BugsnagDiagnostics.cacheTombstone" is null

@skip_below_android_8
Scenario: Sending internal error reports on API >=26
    When I run "InternalReportScenario"
    And I configure the app to run in the "non-crashy" state
    And I relaunch the app
    Then I should receive 1 request
    And the "Bugsnag-Internal-Error" header equals "true"
    And the payload field "apiKey" is null
    And the event "context" equals "Crash Report Deserialization"
    And the event "session" is null
    And the event "breadcrumbs" is null
    And the event "app.type" equals "android"
    And the event "device.osName" equals "android"
    And the event "metaData.BugsnagDiagnostics.filename" is not null
    And the event "metaData.BugsnagDiagnostics.notifierName" equals "Android Bugsnag Notifier"
    And the event "metaData.BugsnagDiagnostics.apiKey" equals "a35a2a72bd230ac0aa0f52715bbdc6aa"
    And the event "metaData.BugsnagDiagnostics.packageName" equals "com.bugsnag.android.mazerunner"
    And the event "metaData.BugsnagDiagnostics.notifierVersion" is not null
    And the event "metaData.BugsnagDiagnostics.fileLength" equals 4
    And the event "metaData.BugsnagDiagnostics.cacheGroup" is false
    And the event "metaData.BugsnagDiagnostics.cacheTombstone" is false

@skip_below_android_8
Scenario: Sending internal error reports with cache tombstone + groups enabled
    When I configure the app to run in the "tombstone" state
    And I run "InternalReportScenario"
    And I configure the app to run in the "non-crashy" state
    And I relaunch the app
    Then I should receive 1 request
    And the "Bugsnag-Internal-Error" header equals "true"
    And the payload field "apiKey" is null
    And the event "context" equals "Crash Report Deserialization"
    And the event "session" is null
    And the event "breadcrumbs" is null
    And the event "app.type" equals "android"
    And the event "device.osName" equals "android"
    And the event "metaData.BugsnagDiagnostics.filename" is not null
    And the event "metaData.BugsnagDiagnostics.notifierName" equals "Android Bugsnag Notifier"
    And the event "metaData.BugsnagDiagnostics.apiKey" equals "a35a2a72bd230ac0aa0f52715bbdc6aa"
    And the event "metaData.BugsnagDiagnostics.packageName" equals "com.bugsnag.android.mazerunner"
    And the event "metaData.BugsnagDiagnostics.notifierVersion" is not null
    And the event "metaData.BugsnagDiagnostics.fileLength" equals 4
    And the event "metaData.BugsnagDiagnostics.cacheGroup" is true
    And the event "metaData.BugsnagDiagnostics.cacheTombstone" is true

Scenario: If a file in the cache directory is deleted before a request completes, zero further requests should be made
    When I run "DeletedReportScenario"
    And I configure the app to run in the "non-crashy" state
    And I relaunch the app
    Then I should receive 0 requests
