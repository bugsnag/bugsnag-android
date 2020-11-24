Feature: Performs basic smoke tests to check that critical functionality is working. This allows
         for a faster feedback loop to check that a simple mistake hasn't entirely broken the
         notifier's error reporting, without having to wait for the full suite to run on CI.

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