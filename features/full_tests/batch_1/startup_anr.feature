Feature: onCreate ANR

# Android 10 Note: Android 10 devices appear to allow much longer times for startup without an ANR
# and then terminate the "misbehaving" app with a KILL (9) signal (almost like the ANR code doesn't
# fire at all). Since we can't cover a KILL signal in a test, we skip Android 10.
@skip_android_10
Scenario: onCreate ANR is reported
  When I run "ConfigureStartupAnrScenario"
  And I relaunch the app after a crash
  And I wait for 30 seconds
  And I clear any error dialogue
  Then I wait to receive an error
  And the exception "errorClass" equals "ANR"
