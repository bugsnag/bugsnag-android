Feature: onCreate ANR

Scenario: onCreate ANR is reported
  When I set an onCreate delay of 30 seconds
  When I restart the ANR app
  And I wait for 30 seconds
  And I clear any error dialogue
  Then I wait to receive an error
  And the exception "errorClass" equals "ANR"
