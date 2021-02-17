Feature: Reports are ignored

Scenario: Exception classname ignored
    When I run "IgnoredExceptionScenario"
    And I wait to receive 1 logs
    Then the "debug" level log message equals "Skipping notification - should not notify for this class"

Scenario: Disabled Exception Handler
    When I run "DisableAutoDetectErrorsScenario" and relaunch the app
    And I configure Bugsnag for "DisableAutoDetectErrorsScenario"
    Then Bugsnag confirms it has no errors to send

Scenario: Changing release stage to exclude the current stage settings before a POSIX signal
    When I run "CXXTrapOutsideReleaseStagesScenario" and relaunch the app
    And I configure Bugsnag for "CXXTrapOutsideReleaseStagesScenario"
    Then Bugsnag confirms it has no errors to send

Scenario: Changing release stage settings to exclude the current stage before a native C++ crash
    When I run "CXXThrowSomethingOutsideReleaseStagesScenario" and relaunch the app
    And I configure Bugsnag for "CXXThrowSomethingOutsideReleaseStagesScenario"
    Then Bugsnag confirms it has no errors to send
