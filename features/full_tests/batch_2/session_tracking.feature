Feature: Session Tracking

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
