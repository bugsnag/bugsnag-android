Feature: onCreate ANR

@skip_android_10
Scenario: onCreate ANR is reported
  When I run "ConfigureStartupAnrScenario"
  And I relaunch the app after a crash
  And I wait for 30 seconds
  And I clear any error dialogue
  Then I wait to receive an error
  And the exception "errorClass" equals "ANR"
