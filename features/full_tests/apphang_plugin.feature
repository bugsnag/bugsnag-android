Feature: AppHang Plugin

  Background:
    Given I clear all persistent data

  Scenario: AppHangPlugin Reports AppHang errors
    When I run "AppHangPluginScenario"
    Then I wait to receive an error
    And the error is valid for the error reporting API version "4.0" for the "Android Bugsnag Notifier" notifier
    And the exception "errorClass" equals "AppHang"
