Feature: Session Tracking

Scenario: Automatic Session Tracking sends
    When I run "AutoSessionScenario" with the defaults
    And I wait for 1 seconds
    Then I should receive a request
    And the request is a valid for the session tracking API
    And the "Bugsnag-API-Key" header equals "a35a2a72bd230ac0aa0f52715bbdc6aa"
    And the payload field "notifier.name" equals "Android Bugsnag Notifier"
    And the payload field "sessions" is an array with 1 element
    And the session "user.id" equals "123"
    And the session "user.email" equals "user@example.com"
    And the session "user.name" equals "Joe Bloggs"
    And the session "id" is not null
    And the session "startedAt" is not null

Scenario: Test cached Session sends
    When I run "SessionCacheScenario" with the defaults
    Then I should receive no requests

    When I force stop the "com.bugsnag.android.mazerunner" Android app
    And I set environment variable "EVENT_TYPE" to "Wait"
    And I start the "com.bugsnag.android.mazerunner" Android app using the "com.bugsnag.android.mazerunner.MainActivity" activity
    Then I should receive a request

    And the request is a valid for the session tracking API
    And the payload field "sessions" is an array with 1 element
    And the session "user.id" equals "123"
    And the session "user.email" equals "user@example.com"
    And the session "user.name" equals "Joe Bloggs"
    And the session "id" is not null
    And the session "startedAt" is not null

Scenario: Manual Session sends
    When I run "ManualSessionScenario" with the defaults
    Then I should receive a request
    And the request is a valid for the session tracking API
    And the "Bugsnag-API-Key" header equals "a35a2a72bd230ac0aa0f52715bbdc6aa"
    And the payload field "notifier.name" equals "Android Bugsnag Notifier"
    And the payload field "sessions" is an array with 1 element
    And the session "user.id" equals "123"
    And the session "user.email" equals "user@example.com"
    And the session "user.name" equals "Joe Bloggs"
    And the session "id" is not null
    And the session "startedAt" is not null

Scenario: Set Auto Capture Sessions sends
    When I run "SessionSetAutoCaptureScenario" with the defaults
    And I wait for 60 seconds
    Then I should receive a request
    And the request is a valid for the session tracking API
