Feature: Reporting User Information

Scenario: Override user details in callback
    When I run "UserCallbackScenario"
    And I wait to receive a request
    And the request is valid for the error reporting API version "4.0" for the "Android Bugsnag Notifier" notifier
    And the exception "message" equals "UserCallbackScenario"
    And the event "user.id" equals "Agent Pink"
    And the event "user.email" equals "bob@example.com"
    And the event "user.name" equals "Zebedee"

Scenario: User fields set as null
    When I run "UserDisabledScenario"
    And I wait to receive a request
    And the request is valid for the error reporting API version "4.0" for the "Android Bugsnag Notifier" notifier
    And the exception "message" equals "UserDisabledScenario"
    And the event "user.id" is null
    And the event "user.email" is null
    And the event "user.name" is null


Scenario: Only User email field set
    When I run "UserEmailScenario"
    And I wait to receive a request
    And the request is valid for the error reporting API version "4.0" for the "Android Bugsnag Notifier" notifier
    And the exception "message" equals "UserEmailScenario"
    And the event "user.id" is null
    And the event "user.email" equals "user@example.com"
    And the event "user.name" is null

Scenario: All user fields set
    When I run "UserEnabledScenario"
    And I wait to receive a request
    And the request is valid for the error reporting API version "4.0" for the "Android Bugsnag Notifier" notifier
    And the exception "message" equals "UserEnabledScenario"
    And the event "user.id" equals "123"
    And the event "user.email" equals "user@example.com"
    And the event "user.name" equals "Joe Bloggs"

Scenario: Only User ID field set
    When I run "UserIdScenario"
    And I wait to receive a request
    And the request is valid for the error reporting API version "4.0" for the "Android Bugsnag Notifier" notifier
    And the exception "message" equals "UserIdScenario"
    And the event "user.id" equals "abc"
    And the event "user.email" is null
    And the event "user.name" is null
