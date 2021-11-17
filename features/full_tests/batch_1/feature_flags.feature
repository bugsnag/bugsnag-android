Feature: Reporting with feature flags

Scenario: Sends handled exception which includes feature flags
  When I run "FeatureFlagScenario"
  Then I wait to receive an error
  And the exception "errorClass" equals "java.lang.RuntimeException"
  And the event "unhandled" is false
  And event 0 contains the feature flag "demo_mode" with no variant
  And event 0 does not contain the feature flag "demo_mode" with no variant

Scenario: Sends handled exception which includes feature flags added in the notify callback
  When I configure the app to run in the "callback" state
  And I run "FeatureFlagScenario"
  Then I wait to receive an error
  And the exception "errorClass" equals "java.lang.RuntimeException"
  And the event "unhandled" is false
  And event 0 contains the feature flag "demo_mode" with no variant
  And event 0 contains the feature flag "sample_group" with variant "a"

Scenario: Sends unhandled exception which includes feature flags added in the notify callback
  When I configure the app to run in the "unhandled callback" state
  And I run "FeatureFlagScenario" and relaunch the app
  And I configure Bugsnag for "FeatureFlagScenario"
  Then I wait to receive an error
  And the exception "errorClass" equals "java.lang.RuntimeException"
  And the event "unhandled" is true
  And event 0 contains the feature flag "demo_mode" with no variant
  And event 0 contains the feature flag "sample_group" with variant "a"
