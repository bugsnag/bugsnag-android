Feature: Session Tracking

Scenario: Automatic Session Tracking sends
    When I run "AutoSessionScenario"
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
    When I configure the app to run in the "offline" state
    And I run "SessionCacheScenario"
    Then I should receive no requests

    When I configure the app to run in the "online" state
    And I relaunch the app
    And I wait for 40 seconds
    Then I should receive 2 requests

    And the request is a valid for the session tracking API
    And the payload field "sessions" is an array with 1 element
    And the session "user.id" equals "123"
    And the session "user.email" equals "user@example.com"
    And the session "user.name" equals "Joe Bloggs"
    And the session "id" is not null
    And the session "startedAt" is not null

Scenario: Manual Session sends
    When I run "ManualSessionScenario"
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
    When I run "SessionSetAutoCaptureScenario"
    And I wait for 60 seconds
    Then I should receive a request
    And the request is a valid for the session tracking API
