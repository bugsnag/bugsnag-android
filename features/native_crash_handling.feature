Feature: Native crash reporting

    Scenario: Dereference a null pointer
        When I run "CXXNullPointerScenario"
        And I configure the app to run in the "non-crashy" state
        And I relaunch the app
        Then I should receive a request
        And the request payload contains a completed native report
        And the exception "errorClass" equals "SIGILL"
        And the exception "type" equals "c"
        And the event "severity" equals "error"
        And the event "unhandled" is true

    Scenario: Stack Overflow
        When I run "CXXStackoverflowScenario"
        And I wait a bit
        And I configure the app to run in the "non-crashy" state
        And I relaunch the app
        Then I should receive a request
        And the request payload contains a completed native report
        And the exception reflects a signal was raised
        And the exception "type" equals "c"
        And the event "severity" equals "error"
        And the event "unhandled" is true

    Scenario: Program trap()
        When I run "CXXTrapScenario"
        And I configure the app to run in the "non-crashy" state
        And I relaunch the app
        Then I should receive a request
        And the request payload contains a completed native report
        And the exception "errorClass" equals "SIGILL"
        And the exception "type" equals "c"
        And the event "severity" equals "error"
        And the event "unhandled" is true

    Scenario: Use pointer after free
        When I run "CXXUseAfterFreeScenario"
        And I configure the app to run in the "non-crashy" state
        And I relaunch the app
        Then I should receive a request
        And the request payload contains a completed native report
        And the exception reflects a signal was raised
        And the exception "type" equals "c"
        And the event "severity" equals "error"
        And the event "unhandled" is true

    Scenario: Reference undefined instruction
        When I run "CXXUndefinedInstructionScenario"
        And I wait a bit
        And I configure the app to run in the "non-crashy" state
        And I relaunch the app
        Then I should receive a request
        And the request payload contains a completed native report
        And the exception reflects a signal was raised
        And the exception "type" equals "c"
        And the event "severity" equals "error"
        And the event "unhandled" is true

    Scenario: Divide by zero
        When I run "CXXDivideByZeroScenario"
        And I configure the app to run in the "non-crashy" state
        And I relaunch the app
        Then I should receive a request
        And the request payload contains a completed native report
        And the exception "errorClass" equals "SIGFPE"
        And the exception "type" equals "c"
        And the event "severity" equals "error"
        And the event "unhandled" is true

    Scenario: Program abort()
        When I run "CXXAbortScenario"
        And I configure the app to run in the "non-crashy" state
        And I relaunch the app
        Then I should receive a request
        And the request payload contains a completed native report
        And the exception "errorClass" equals one of:
            | SIGABRT |
            | SIGSEGV |
        And the exception "type" equals "c"
        And the event "severity" equals "error"
        And the event "unhandled" is true

    Scenario: Raise SIGBUS

    Scenario: Raise SIGTRAP
