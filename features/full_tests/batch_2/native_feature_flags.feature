Feature: Synchronizing feature flags to the native layer

  Scenario: Feature flags are synchronized to the native layer
    When I run "CXXFeatureFlagNativeCrashScenario"
    And I configure Bugsnag for "CXXFeatureFlagNativeCrashScenario"
    And I wait to receive an error
    And the error payload contains a completed unhandled native report
    And the exception "errorClass" equals "SIGILL"
    And event 0 contains the feature flag "demo_mode" with no variant
    And event 0 contains the feature flag "sample_group" with variant "a"
