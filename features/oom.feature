Feature: Reporting OOMs

Scenario: Out of Memory Error captured
    When I run "OomScenario" and relaunch the app
    And I configure Bugsnag for "OomScenario"
    Then I wait to receive a request
    And the request is valid for the error reporting API version "4.0" for the "Android Bugsnag Notifier" notifier
    And the payload field "notifier.name" equals "Android Bugsnag Notifier"
    And the payload field "events" is an array with 1 elements
    And the exception "errorClass" equals "java.lang.OutOfMemoryError"
