Feature: Reporting app version

Scenario: Test handled Android Exception
    When I run "AppVersionScenario"
    Then I wait to receive a request
    And the request is valid for the error reporting API version "4.0" for the "Android Bugsnag Notifier" notifier
    And the exception "errorClass" equals "java.lang.RuntimeException"
    And the exception "message" equals "AppVersionScenario"
    And the event "app.version" equals "1.2.3.abc"
