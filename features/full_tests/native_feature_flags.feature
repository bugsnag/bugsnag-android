Feature: Synchronizing feature flags to the native layer

  Background:
    Given I clear all persistent data

  Scenario: Feature flags are synchronized to the native layer
    When I run "CXXFeatureFlagNativeCrashScenario" and relaunch the crashed app
    And I configure Bugsnag for "CXXFeatureFlagNativeCrashScenario"
    And I wait to receive an error
    And the error payload contains a completed unhandled native report
    And the exception "errorClass" equals one of:
      | SIGILL  |
      | SIGTRAP |
    And event 0 contains the feature flag "demo_mode" with no variant
    And event 0 contains the feature flag "sample_group" with variant "a"
    And event 0 does not contain the feature flag "should_not_be_reported_1"
    And event 0 does not contain the feature flag "should_not_be_reported_2"
    And event 0 does not contain the feature flag "should_not_be_reported_3"

  Scenario: clearFeatureFlags() is synchronized to the native layer
    When I configure the app to run in the "cleared" state
    And I run "CXXFeatureFlagNativeCrashScenario" and relaunch the crashed app
    And I configure Bugsnag for "CXXFeatureFlagNativeCrashScenario"
    And I wait to receive an error
    And the error payload contains a completed unhandled native report
    And the exception "errorClass" equals one of:
      | SIGILL  |
      | SIGTRAP |
    And event 0 has no feature flags

  Scenario: Sends feature flags added in OnSend Callbacks
    When I run "CXXFeatureFlagNativeCrashScenario"
    And I relaunch the app after a crash
    And I configure the app to run in the "onsend" state
    And I configure Bugsnag for "CXXFeatureFlagNativeCrashScenario"
    Then I wait to receive an error
    And the exception "errorClass" equals one of:
      | SIGILL  |
      | SIGTRAP |
    And the event "unhandled" is true
    And event 0 contains the feature flag "demo_mode" with no variant
    And event 0 contains the feature flag "on_send_callback" with no variant
    And event 0 does not contain the feature flag "should_not_be_reported_1"
    And event 0 does not contain the feature flag "should_not_be_reported_2"
    And event 0 does not contain the feature flag "should_not_be_reported_3"
