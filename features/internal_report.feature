Feature: Sending internal error reports

Scenario: Send a report about an error triggered within the notifier
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
    And the event "metaData.BugsnagDiagnostics.cacheTombstone" is false
    And the event "metaData.BugsnagDiagnostics.filename" is not null
    And the event "metaData.BugsnagDiagnostics.notifierName" equals "Android Bugsnag Notifier"
    And the event "metaData.BugsnagDiagnostics.apiKey" equals "a35a2a72bd230ac0aa0f52715bbdc6aa"
    And the event "metaData.BugsnagDiagnostics.cacheGroup" is false
    And the event "metaData.BugsnagDiagnostics.packageName" equals "com.bugsnag.android.mazerunner"
    And the event "metaData.BugsnagDiagnostics.notifierVersion" is not null
    And the event "metaData.BugsnagDiagnostics.fileLength" equals 4
