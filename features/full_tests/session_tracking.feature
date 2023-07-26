Feature: Session Tracking

  Background:
    Given I clear all persistent data

  Scenario: User is persisted between sessions
    When I run "SessionPersistUserScenario"
    And I wait to receive a session
    Then the session is valid for the session reporting API version "1.0" for the "Android Bugsnag Notifier" notifier
    And the session Bugsnag-Integrity header is valid
    And the session "user.id" equals "12345"
    And the session "user.email" equals "test@test.test"
    And the session "user.name" equals "test user"
    When I discard the oldest session

    And I close and relaunch the app
    And I configure the app to run in the "no_user" state
    And I run "SessionPersistUserScenario"
    And I wait to receive a session
    Then the session is valid for the session reporting API version "1.0" for the "Android Bugsnag Notifier" notifier
    And the session Bugsnag-Integrity header is valid
    And the session "user.id" equals "12345"
    And the session "user.email" equals "test@test.test"
    And the session "user.name" equals "test user"

  Scenario: User is not persisted between sessions
    When I run "SessionPersistUserDisabledScenario"
    And I wait to receive a session
    Then the session is valid for the session reporting API version "1.0" for the "Android Bugsnag Notifier" notifier
    And the session Bugsnag-Integrity header is valid
    And the session "user.id" equals "12345"
    And the session "user.email" equals "test@test.test"
    And the session "user.name" equals "test user"
    When I discard the oldest session

    And I close and relaunch the app
    And I configure the app to run in the "no_user" state
    And I run "SessionPersistUserDisabledScenario"
    And I wait to receive a session
    Then the session is valid for the session reporting API version "1.0" for the "Android Bugsnag Notifier" notifier
    And the session Bugsnag-Integrity header is valid
    And the session "user.id" is not null
    And the session "user.name" is null
    And the session "user.email" is null

  Scenario: Session apikey can be reset
    When I run "SessionApiKeyResetScenario"
    And I wait to receive a session
    Then the session Bugsnag-Integrity header is valid
    And the session "Bugsnag-Api-Key" header equals "TEST APIKEY"
    And the session "bugsnag-payload-version" header equals "1.0"
    And the session "Content-Type" header equals "application/json"
    And the session "Bugsnag-Sent-At" header is a timestamp

    And the session payload field "notifier.name" equals "Android Bugsnag Notifier"
    And the session payload field "notifier.url" is not null
    And the session payload field "notifier.version" is not null

    And the session payload field "app" is not null
    And the session payload field "device" is not null
