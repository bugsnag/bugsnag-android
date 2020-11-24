Feature: Reports are ignored

Scenario: Exception classname ignored
    When I run "IgnoredExceptionScenario"
    And I wait for 2 seconds
    Then I should receive no requests

Scenario: Disabled Exception Handler
    When I run "DisableAutoDetectErrorsScenario"
    And I wait for 2 seconds
    Then I should receive no requests

Scenario: Changing release stage to exclude the current stage settings before a POSIX signal
    When I run "CXXTrapOutsideReleaseStagesScenario" and relaunch the app
    And I configure the app to run in the "non-crashy" state
    And I run "CXXTrapOutsideReleaseStagesScenario"
    Then I should receive no requests

Scenario: Changing release stage settings to exclude the current stage before a native C++ crash
    When I run "CXXThrowSomethingOutsideReleaseStagesScenario" and relaunch the app
    And I configure the app to run in the "non-crashy" state
    And I run "CXXThrowSomethingOutsideReleaseStagesScenario"
    Then I should receive no requests
