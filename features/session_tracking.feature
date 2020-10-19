Feature: Session Tracking

Scenario: Automatic Session Tracking sends
    When I run "AutoSessionScenario"
    And I wait to receive a request
    Then the request is valid for the session reporting API version "1.0" for the "Android Bugsnag Notifier" notifier
    And the payload field "notifier.name" equals "Android Bugsnag Notifier"
    And the payload field "sessions" is an array with 1 elements
    And the session "user.id" equals "123"
    And the session "user.email" equals "user@example.com"
    And the session "user.name" equals "Joe Bloggs"
    And the session "id" is not null
    And the session "startedAt" is not null

Scenario: Manual Session sends
    When I run "ManualSessionScenario"
    And I wait to receive a request
    Then the request is valid for the session reporting API version "1.0" for the "Android Bugsnag Notifier" notifier
    And the payload field "sessions" is an array with 1 elements
    And the session "user.id" equals "123"
    And the session "user.email" equals "user@example.com"
    And the session "user.name" equals "Joe Bloggs"
    And the session "id" is not null
    And the session "startedAt" is not null

Scenario: Set Auto Capture Sessions sends
    When I run "SessionSetAutoCaptureScenario"
    And I wait to receive a request
    Then the request is valid for the session reporting API version "1.0" for the "Android Bugsnag Notifier" notifier

Scenario: User is persisted between sessions
    When I run "SessionPersistUserScenario"
    And I wait to receive a request
    Then the request is valid for the session reporting API version "1.0" for the "Android Bugsnag Notifier" notifier
    And the session "user.id" equals "12345"
    And the session "user.email" equals "test@test.test"
    And the session "user.name" equals "test user"
    When I discard the oldest request
    And I relaunch the app
    And I configure the app to run in the "no_user" state
    And I run "SessionPersistUserScenario"
    And I wait to receive a request
    Then the request is valid for the session reporting API version "1.0" for the "Android Bugsnag Notifier" notifier
    And the session "user.id" equals "12345"
    And the session "user.email" equals "test@test.test"
    And the session "user.name" equals "test user"

Scenario: User is not persisted between sessions
    When I run "SessionPersistUserDisabledScenario"
    And I wait to receive a request
    Then the request is valid for the session reporting API version "1.0" for the "Android Bugsnag Notifier" notifier
    And the session "user.id" equals "12345"
    And the session "user.email" equals "test@test.test"
    And the session "user.name" equals "test user"
    When I discard the oldest request
    And I relaunch the app
    And I configure the app to run in the "no_user" state
    And I run "SessionPersistUserDisabledScenario"
    And I wait to receive a request
    Then the request is valid for the session reporting API version "1.0" for the "Android Bugsnag Notifier" notifier
    And the session "user.id" is not null
    And the session "user.name" is null
    And the session "user.email" is null