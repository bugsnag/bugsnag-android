Feature: Communicating events between notifiers

    Other Bugsnag libraries include bugsnag-android as a dependency for
    capturing native Java and C/C++ crashes, but may have additional events to
    report, both handled and unhandled. These events should be reported
    correctly when using bugsnag-android as the delivery layer.

    Scenario: Report a handled event through internalNotify()
        When I run "HandledInternalNotifyScenario"
        Then I should receive a request
        And the request is valid for the error reporting API
        And the "Bugsnag-API-Key" header equals "a35a2a72bd230ac0aa0f52715bbdc6aa"
        And the event "unhandled" is false
        And the exception "errorClass" equals "Handled Error!"
        And the exception "message" equals "all work and no play"
        And the exception "type" equals "JS"
        And the exception "specialField" equals "layer cake"
        And the "method" of stack frame 0 equals "foo()"
        And the "file" of stack frame 0 equals "src/Giraffe.js"
        And the "lineNumber" of stack frame 0 equals 200
        And the "extraNumber" of stack frame 0 equals 43
        And the "method" of stack frame 1 equals "bar()"
        And the "file" of stack frame 1 equals "parser.js"
        And the "someAddress" of stack frame 1 equals "0xea14616b0"
        And the "lineNumber" of stack frame 1 is null
        And the "method" of stack frame 2 is null
        And the "file" of stack frame 2 is null
        And the "lineNumber" of stack frame 2 is null
        And the "someAddress" of stack frame 2 equals "0x000000000"

    Scenario: Report a handled event through internalNotify() while offline
        When I configure the app to run in the "offline" state
        And I run "CachedHandledInternalNotifyScenario"
        And I configure the app to run in the "online" state
        And I relaunch the app
        Then I should receive a request
        And the request is valid for the error reporting API
        And the "Bugsnag-API-Key" header equals "a35a2a72bd230ac0aa0f52715bbdc6aa"
        And the event "unhandled" is false
        And the exception "errorClass" equals "Handled Error!"
        And the exception "message" equals "all work and no play"
        And the exception "type" equals "JS"
        And the exception "specialField" equals "layer cake"
        And the "method" of stack frame 0 equals "foo()"
        And the "file" of stack frame 0 equals "src/Giraffe.js"
        And the "lineNumber" of stack frame 0 equals 200
        And the "extraNumber" of stack frame 0 equals 43
        And the "method" of stack frame 1 equals "bar()"
        And the "file" of stack frame 1 equals "parser.js"
        And the "someAddress" of stack frame 1 equals "0xea14616b0"
        And the "lineNumber" of stack frame 1 is null
        And the "method" of stack frame 2 is null
        And the "file" of stack frame 2 is null
        And the "lineNumber" of stack frame 2 is null
        And the "someAddress" of stack frame 2 equals "0x000000000"
