Feature: Native crash reporting

    Scenario: Dereference a null pointer
        When I run "CXXNullPointerScenario"
        And I configure the app to run in the "non-crashy" state
        And I relaunch the app
        And I wait for 10 seconds
        Then I should receive a request
        And the request is a valid for the error reporting API
        And the exception "errorClass" equals "SIGILL"
        And the exception "type" equals "c"
        And the payload field "events.0.exceptions.0.stacktrace" is a non-empty array
        And the event "severity" equals "error"

    Scenario: Stack Overflow
        When I run "CXXStackoverflowScenario"
        And I wait for 10 seconds
        And I configure the app to run in the "non-crashy" state
        And I relaunch the app
        And I wait for 10 seconds
        Then I should receive a request
        And the request is a valid for the error reporting API
        And the exception "errorClass" equals "SIGSEGV"
        And the exception "type" equals "c"
        And the payload field "events.0.exceptions.0.stacktrace" is a non-empty array
        And the event "severity" equals "error"

    Scenario: Program trap()
        When I run "CXXTrapScenario"
        And I configure the app to run in the "non-crashy" state
        And I relaunch the app
        And I wait for 10 seconds
        Then I should receive a request
        And the request is a valid for the error reporting API
        And the exception "errorClass" equals "SIGILL"
        And the exception "type" equals "c"
        And the payload field "events.0.exceptions.0.stacktrace" is a non-empty array
        And the event "severity" equals "error"

    Scenario: Use pointer after free
        When I run "CXXUseAfterFreeScenario"
        And I configure the app to run in the "non-crashy" state
        And I relaunch the app
        And I wait for 10 seconds
        Then I should receive a request
        And the request is a valid for the error reporting API
        And the exception "errorClass" equals "SIGSEGV"
        And the exception "type" equals "c"
        And the payload field "events.0.exceptions.0.stacktrace" is a non-empty array
        And the event "severity" equals "error"

    Scenario: Reference undefined instruction
        When I run "CXXUndefinedInstructionScenario"
        And I configure the app to run in the "non-crashy" state
        And I relaunch the app
        And I wait for 10 seconds
        Then I should receive a request
        And the request is a valid for the error reporting API
        And the exception "errorClass" equals "SIGSEGV"
        And the exception "type" equals "c"
        And the payload field "events.0.exceptions.0.stacktrace" is a non-empty array
        And the event "severity" equals "error"

    Scenario: Divide by zero
        When I run "CXXDivideByZeroScenario"
        And I configure the app to run in the "non-crashy" state
        And I relaunch the app
        And I wait for 10 seconds
        Then I should receive a request
        And the request is a valid for the error reporting API
        And the exception "errorClass" equals "SIGFPE"
        And the exception "type" equals "c"
        And the payload field "events.0.exceptions.0.stacktrace" is a non-empty array
        And the event "severity" equals "error"

    Scenario: Program abort()
        When I run "CXXAbortScenario"
        And I configure the app to run in the "non-crashy" state
        And I relaunch the app
        And I wait for 10 seconds
        Then I should receive a request
        And the request is a valid for the error reporting API
        And the exception "errorClass" equals "SIGABRT"
        And the exception "type" equals "c"
        And the payload field "events.0.exceptions.0.stacktrace" is a non-empty array
        And the event "severity" equals "error"
