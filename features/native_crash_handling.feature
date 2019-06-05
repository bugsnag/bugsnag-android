Feature: Native crash reporting

    Scenario: Dereference a null pointer
        When I run "CXXNullPointerScenario"
        And I wait for 2 seconds
        And I relaunch the app
        And I wait to receive a request
        And the request payload contains a completed native report
        And the exception "errorClass" equals "SIGILL"
        And the exception "message" equals "Illegal instruction"
        And the exception "type" equals "c"
        And the event "severity" equals "error"
        And the event "unhandled" is true
        And the event "app.binaryArch" is not null
        And the payload field "events.0.device.cpuAbi" is a non-empty array

    # This scenario will not pass on API levels < 18, as stack corruption
    # is handled without calling atexit handlers, etc.
    # In the device logs you will see:
    # system/bin/app_process([proc id]): stack corruption detected: aborted
    # Original code here:
    # https://android.googlesource.com/platform/bionic/+/d0f2b7e7e65f19f978c59abcbb522c08e76b1508/libc/bionic/ssp.c
    # Refactored here to use abort() on newer versions:
    # https://android.googlesource.com/platform/bionic/+/fb7eb5e07f43587c2bedf2aaa53b21fa002417bb
    @skip_below_api18
    Scenario: Stack buffer overflow
        When I run "CXXStackoverflowScenario"
        And I wait for 2 seconds
        And I relaunch the app
        And I wait to receive a request
        And the request payload contains a completed native report
        And the exception reflects a signal was raised
        And the exception "type" equals "c"
        And the event "severity" equals "error"
        And the event "unhandled" is true

    Scenario: Program trap()
        When I run "CXXTrapScenario"
        And I wait for 2 seconds
        And I relaunch the app
        And I wait to receive a request
        And the request payload contains a completed native report
        And the exception "errorClass" equals "SIGILL"
        And the exception "message" equals "Illegal instruction"
        And the exception "type" equals "c"
        And the event "severity" equals "error"
        And the event "unhandled" is true

    Scenario: Write to read-only memory
        When I run "CXXWriteReadOnlyMemoryScenario"
        And I wait for 2 seconds
        And I relaunch the app
        And I wait to receive a request
        And the exception "errorClass" equals "SIGSEGV"
        And the exception "message" equals "Segmentation violation (invalid memory reference)"
        And the exception "type" equals "c"
        And the event "severity" equals "error"
        And the event "unhandled" is true

    Scenario: Double free() allocated memory
        When I run "CXXDoubleFreeScenario"
        And I wait for 2 seconds
        And I relaunch the app
        And I wait to receive a request
        And the exception "errorClass" equals "SIGSEGV"
        And the exception "message" equals "Segmentation violation (invalid memory reference)"
        And the exception "type" equals "c"
        And the event "severity" equals "error"
        And the event "unhandled" is true

    Scenario: Improper object type cast
        When I run "CXXImproperTypecastScenario"
        And I wait for 2 seconds
        And I relaunch the app
        And I wait to receive a request
        And the exception "errorClass" equals "SIGSEGV"
        And the exception "message" equals "Segmentation violation (invalid memory reference)"
        And the exception "type" equals "c"
        And the event "severity" equals "error"
        And the event "unhandled" is true

    Scenario: Divide by zero
        When I run "CXXDivideByZeroScenario"
        And I wait for 2 seconds
        And I relaunch the app
        And I wait to receive a request
        And the request payload contains a completed native report
        And the exception "errorClass" equals "SIGFPE"
        And the exception "message" equals "Floating-point exception"
        And the exception "type" equals "c"
        And the event "severity" equals "error"
        And the event "unhandled" is true

    Scenario: Program abort()
        When I run "CXXAbortScenario"
        And I wait for 2 seconds
        And I relaunch the app
        And I wait to receive a request
        And the request payload contains a completed native report
        And the exception "errorClass" equals one of:
            | SIGABRT |
            | SIGSEGV |
        And the exception "message" equals one of:
            | Abort program |
            | Segmentation violation (invalid memory reference) |
        And the exception "type" equals "c"
        And the event "severity" equals "error"
        And the event "unhandled" is true

    Scenario: Raise SIGILL
        When I run "CXXSigillScenario"
        And I wait for 2 seconds
        And I relaunch the app
        And I wait to receive a request
        And the request payload contains a completed native report
        And the exception "errorClass" equals "SIGILL"
        And the exception "message" equals "Illegal instruction"
        And the exception "type" equals "c"
        And the event "severity" equals "error"
        And the event "unhandled" is true

    Scenario: Raise SIGSEGV
        When I run "CXXSigsegvScenario"
        And I wait for 2 seconds
        And I relaunch the app
        And I wait to receive a request
        And the request payload contains a completed native report
        And the exception "errorClass" equals "SIGSEGV"
        And the exception "message" equals "Segmentation violation (invalid memory reference)"
        And the exception "type" equals "c"
        And the event "severity" equals "error"
        And the event "unhandled" is true

    Scenario: Raise SIGABRT
        When I run "CXXSigabrtScenario"
        And I wait for 2 seconds
        And I relaunch the app
        And I wait to receive a request
        And the request payload contains a completed native report
        And the exception "errorClass" equals "SIGABRT"
        And the exception "message" equals "Abort program"
        And the exception "type" equals "c"
        And the event "severity" equals "error"
        And the event "unhandled" is true

    Scenario: Raise SIGBUS
        When I run "CXXSigbusScenario"
        And I wait for 2 seconds
        And I relaunch the app
        And I wait to receive a request
        And the request payload contains a completed native report
        And the exception "errorClass" equals "SIGBUS"
        And the exception "message" equals "Bus error (bad memory access)"
        And the exception "type" equals "c"
        And the event "severity" equals "error"
        And the event "unhandled" is true

    Scenario: Raise SIGFPE
        When I run "CXXSigfpeScenario"
        And I wait for 2 seconds
        And I relaunch the app
        And I wait to receive a request
        And the request payload contains a completed native report
        And the exception "errorClass" equals "SIGFPE"
        And the exception "message" equals "Floating-point exception"
        And the exception "type" equals "c"
        And the event "severity" equals "error"
        And the event "unhandled" is true

    Scenario: Raise SIGTRAP
        When I run "CXXSigtrapScenario"
        And I wait for 2 seconds
        And I relaunch the app
        And I wait to receive a request
        And the request payload contains a completed native report
        And the exception "errorClass" equals "SIGTRAP"
        And the exception "message" equals "Trace/breakpoint trap"
        And the exception "type" equals "c"
        And the event "severity" equals "error"
        And the event "unhandled" is true

    Scenario: Undefined JNI method
        When I run "UnsatisfiedLinkErrorScenario"
        And I wait for 2 seconds
        And I relaunch the app
        And I wait to receive a request
        And the report contains the required fields
        And the exception "errorClass" equals "java.lang.UnsatisfiedLinkError"
        And the exception "type" equals "android"
        And the event "severity" equals "error"
        And the event "unhandled" is true

    Scenario: Causing a crash in a separate library
        When I run "CXXExternalStackElementScenario"
        And I wait for 2 seconds
        And I relaunch the app
        And I wait to receive a request
        And the request payload contains a completed native report
        And the event "severity" equals "error"
        And the event "unhandled" is true
        And the exception "errorClass" equals one of:
            | SIGILL  |
            | SIGTRAP |
        And the exception "message" equals one of:
            | Illegal instruction   |
            | Trace/breakpoint trap |
        And the exception "type" equals "c"
        And the first significant stack frame methods and files should match:
            | something_innocuous | libmonochrome.so |
            | Java_com_bugsnag_android_mazerunner_scenarios_CXXExternalStackElementScenario_crash | libentrypoint.so |

    Scenario: Throwing an exception in C++
        When I run "CXXExceptionScenario"
        And I wait for 2 seconds
        And I relaunch the app
        And I wait to receive a request
        And the request payload contains a completed native report
        And the event "severity" equals "error"
        And the event "unhandled" is true
        And the exception "errorClass" equals "PSt13runtime_error"
        And the exception "message" equals "How about NO"
        And the first significant stack frame methods and files should match:
            | run_away(bool)       | libentrypoint.so |
            | trigger_an_exception | libentrypoint.so |
            | Java_com_bugsnag_android_mazerunner_scenarios_CXXExceptionScenario_crash | libentrypoint.so |

    Scenario: Throwing an object in C++
        When I run "CXXThrowSomethingScenario"
        And I wait for 2 seconds
        And I relaunch the app
        And I wait to receive a request
        And the request payload contains a completed native report
        And the event "severity" equals "error"
        And the event "unhandled" is true
        And the exception "errorClass" equals "i"
        And the exception "message" equals "42"
        And the first significant stack frame methods and files should match:
            | run_back(int, int) | libentrypoint.so |
            | throw_an_object    | libentrypoint.so |
            | Java_com_bugsnag_android_mazerunner_scenarios_CXXThrowSomethingScenario_crash | libentrypoint.so |
