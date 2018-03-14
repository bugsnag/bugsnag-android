Feature: Reporting app version

Scenario: Test handled Android Exception
    When I run "AppVersionScenario" with the defaults
    Then I should receive a request
    And the request is a valid for the error reporting API
    And the exception "errorClass" equals "java.lang.RuntimeException"
    And the exception "message" equals "AppVersionScenario"
    And the event "app.version" equals "1.2.3.abc"
