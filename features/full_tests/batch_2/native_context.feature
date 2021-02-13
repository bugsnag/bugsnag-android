Feature: Native Context API

    Scenario: Changing intents followed by notifying in C
        When I run "CXXAutoContextScenario"
        And I wait to receive an error
        Then the error payload contains a completed handled native report
        And the event "severity" equals "info"
        And the event "context" equals "SecondActivity"
        And the exception "errorClass" equals "Hello hello"
        And the exception "message" equals "This is a new world"
        And the event "unhandled" is false

    Scenario: Update context in Java followed by crashing in C
        When I run "CXXUpdateContextCrashScenario" and relaunch the app
        And I configure Bugsnag for "CXXUpdateContextCrashScenario"
        And I wait to receive an error
        Then the error payload contains a completed handled native report
        And the event "severity" equals "error"
        And the event "context" equals "Everest"
        And the exception "errorClass" equals one of:
            | SIGILL |
            | SIGTRAP |
        And the event "unhandled" is true
