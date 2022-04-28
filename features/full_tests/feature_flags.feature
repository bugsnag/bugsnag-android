Feature: Reporting with feature flags

  Background:
    Given I clear all persistent data

  Scenario: Sends handled exception which includes feature flags
    When I run "FeatureFlagScenario"
    Then I wait to receive an error
    And the exception "errorClass" equals "java.lang.RuntimeException"
    And the event "unhandled" is false
    And event 0 contains the feature flag "demo_mode" with no variant
    And event 0 does not contain the feature flag "should_not_be_reported_1"
    And event 0 does not contain the feature flag "should_not_be_reported_2"
    And event 0 does not contain the feature flag "should_not_be_reported_3"

  Scenario: Sends handled exception which includes feature flags added in the notify callback
    When I configure the app to run in the "callback" state
    And I run "FeatureFlagScenario"
    Then I wait to receive an error
    And the exception "errorClass" equals "java.lang.RuntimeException"
    And the event "unhandled" is false
    And event 0 contains the feature flag "demo_mode" with no variant
    And event 0 contains the feature flag "sample_group" with variant "a"
    And event 0 does not contain the feature flag "should_not_be_reported_1"
    And event 0 does not contain the feature flag "should_not_be_reported_2"
    And event 0 does not contain the feature flag "should_not_be_reported_3"

  Scenario: Sends unhandled exception which includes feature flags added in the notify callback
    When I configure the app to run in the "unhandled callback" state
    And I run "FeatureFlagScenario" and relaunch the crashed app
    And I configure Bugsnag for "FeatureFlagScenario"
    Then I wait to receive an error
    And the exception "errorClass" equals "java.lang.RuntimeException"
    And the event "unhandled" is true
    And event 0 contains the feature flag "demo_mode" with no variant
    And event 0 contains the feature flag "sample_group" with variant "a"
    And event 0 does not contain the feature flag "should_not_be_reported_1"
    And event 0 does not contain the feature flag "should_not_be_reported_2"
    And event 0 does not contain the feature flag "should_not_be_reported_3"

  Scenario: Sends no feature flags after clearFeatureFlags()
    When I configure the app to run in the "cleared" state
    And I run "FeatureFlagScenario"
    Then I wait to receive an error
    And the exception "errorClass" equals "java.lang.RuntimeException"
    And the event "unhandled" is false
    And event 0 has no feature flags

  Scenario: Sends feature flags added in OnSend Callbacks
    When I configure the app to run in the "onsend" state
    And I run "FeatureFlagScenario"
    Then I wait to receive an error
    And the exception "errorClass" equals "java.lang.RuntimeException"
    And the event "unhandled" is false
    And event 0 contains the feature flag "demo_mode" with no variant
    And event 0 contains the feature flag "on_send_callback" with no variant
    And event 0 does not contain the feature flag "should_not_be_reported_1"
    And event 0 does not contain the feature flag "should_not_be_reported_2"
    And event 0 does not contain the feature flag "should_not_be_reported_3"
