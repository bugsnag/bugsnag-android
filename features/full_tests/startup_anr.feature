Feature: onCreate ANR

  Background:
    Given I clear all persistent data

# Android 10 Note: Android 10 devices appear to allow much longer times for startup without an ANR
# and then terminate the "misbehaving" app with a KILL (9) signal (almost like the ANR code doesn't
# fire at all). Since we can't cover a KILL signal in a test, we skip Android 10.
  @skip_android_10
# Android 13 Note: we no longer have permission to inject BACK button events, which are used to
# trigger the ANR - so the test is not valid on Android 13 either
  @skip_android_13
  Scenario: onCreate ANR is reported
    When I clear any error dialogue
    And I run "ConfigureStartupAnrScenario"
    And I relaunch the app after a crash
    Then I wait to receive an error
    And the exception "errorClass" equals "ANR"
