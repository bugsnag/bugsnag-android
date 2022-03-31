Feature: Native API

  Background:
    Given I clear all persistent data

  Scenario: Set extraordinarily long app information
    When I run "CXXExtraordinaryLongStringScenario" and relaunch the crashed app
    And I configure Bugsnag for "CXXExtraordinaryLongStringScenario"
    And I wait to receive an error
    And the error payload contains a completed handled native report
    And the exception "errorClass" equals one of:
      | SIGILL  |
      | SIGTRAP |
    And the event "app.version" equals "22.312.749.78.300.810.24.167.32"
    And the event "context" equals "ObservableSessionInitializerStringParserStringSessionProxyGloba"
    And the event "unhandled" is true

  Scenario: Use the NDK methods without "env" after calling "bugsnag_start"
    When I run "CXXStartScenario"
    And I wait to receive an error
    Then the error payload contains a completed handled native report
    And the event "unhandled" is false
    And the exception "errorClass" equals "Start scenario"
    And the exception "message" equals "Testing env"
    And the event "severity" equals "info"
    And the event has a "log" breadcrumb named "Start scenario crumb"
