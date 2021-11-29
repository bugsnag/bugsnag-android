Feature: Synchronizing feature flags to the native layer

  Scenario: Feature flags are synchronized to the native layer
    When I run "CXXFeatureFlagNativeCrashScenario"
    And I configure Bugsnag for "CXXFeatureFlagNativeCrashScenario"
    And I wait to receive an error
    And the error payload contains a completed unhandled native report
    And the exception "errorClass" equals "SIGILL"
    And event 0 contains the feature flag "demo_mode" with no variant
    And event 0 contains the feature flag "sample_group" with variant "a"
    And event 0 does not contain the feature flag "should_not_be_reported_1"
    And event 0 does not contain the feature flag "should_not_be_reported_2"
    And event 0 does not contain the feature flag "should_not_be_reported_3"

  Scenario: clearFeatureFlags() is synchronized to the native layer
    When I configure the app to run in the "cleared" state
    And I run "CXXFeatureFlagNativeCrashScenario"
    And I configure Bugsnag for "CXXFeatureFlagNativeCrashScenario"
    And I wait to receive an error
    And the error payload contains a completed unhandled native report
    And the exception "errorClass" equals "SIGILL"
    And event 0 has no feature flags
