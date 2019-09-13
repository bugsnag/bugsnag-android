Feature: Reports are ignored

Scenario: Exception classname ignored
    When I run "IgnoredExceptionScenario"
    Then I should receive no requests

Scenario: Disabled Exception Handler
    When I run "DisableAutoNotifyScenario"
    Then I should receive no requests

Scenario: Disabling native crash reporting before a native C++ crash
    When I run "CXXThrowSomethingLaterDisabledScenario"
    And I configure the app to run in the "non-crashy" state
    And I relaunch the app
    Then I should receive no requests

Scenario: Disabling native crash reporting before a POSIX signal
    When I run "CXXTrapLaterDisabledScenario"
    And I configure the app to run in the "non-crashy" state
    And I relaunch the app
    Then I should receive no requests

Scenario: Reenabling native crash reporting before a native C++ crash
    When I run "CXXThrowSomethingReenabledScenario"
    And I configure the app to run in the "non-crashy" state
    And I relaunch the app
    Then I should receive a request

Scenario: Changing release stage to exclude the current stage settings before a POSIX signal
    When I run "CXXTrapOutsideReleaseStagesScenario"
    And I configure the app to run in the "non-crashy" state
    And I relaunch the app
    Then I should receive no requests

Scenario: Changing release stage settings to exclude the current stage before a native C++ crash
    When I run "CXXThrowSomethingOutsideReleaseStagesScenario"
    And I configure the app to run in the "non-crashy" state
    And I relaunch the app
    Then I should receive no requests
