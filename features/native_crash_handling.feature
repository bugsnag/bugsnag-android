Feature: Native crash reporting

    Scenario: Dereference a null pointer
        When I run "CXXNullPointerScenario" and relaunch the app
        And I configure the app to run in the "non-crashy" state
        And I configure Bugsnag for "CXXNullPointerScenario"
        And I wait to receive a request
        And the request payload contains a completed unhandled native report
        And the exception "errorClass" equals one of:
          | SIGILL |
          | SIGTRAP |
        And the exception "message" equals one of:
            | Illegal instruction   |
            | Trace/breakpoint trap |
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
        When I run "CXXStackoverflowScenario" and relaunch the app
        And I configure the app to run in the "non-crashy" state
        And I configure Bugsnag for "CXXStackoverflowScenario"
        And I wait to receive a request
        And the request payload contains a completed unhandled native report
        And the exception reflects a signal was raised
        And the exception "type" equals "c"
        And the event "severity" equals "error"
        And the event "unhandled" is true

    Scenario: Program trap()
        When I run "CXXTrapScenario" and relaunch the app
        And I configure the app to run in the "non-crashy" state
        And I configure Bugsnag for "CXXTrapScenario"
        And I wait to receive a request
        And the request payload contains a completed unhandled native report
        And the exception "errorClass" equals one of:
          | SIGILL |
          | SIGTRAP |
        And the exception "message" equals one of:
            | Illegal instruction   |
            | Trace/breakpoint trap |
        And the exception "type" equals "c"
        And the event "severity" equals "error"
        And the event "unhandled" is true

    Scenario: Write to read-only memory
        When I run "CXXWriteReadOnlyMemoryScenario" and relaunch the app
        And I configure the app to run in the "non-crashy" state
        And I configure Bugsnag for "CXXWriteReadOnlyMemoryScenario"
        And I wait to receive a request
        And the exception "errorClass" equals "SIGSEGV"
        And the exception "message" equals "Segmentation violation (invalid memory reference)"
        And the exception "type" equals "c"
        And the event "severity" equals "error"
        And the event "unhandled" is true

    # Skip due to an issue on later Android platforms - [PLAT-5465]
    @skip_android_10 @skip_android_11
    Scenario: Double free() allocated memory
        When I run "CXXDoubleFreeScenario" and relaunch the app
        And I configure the app to run in the "non-crashy" state
        And I configure Bugsnag for "CXXDoubleFreeScenario"
        And I wait to receive a request
        And the exception "errorClass" equals "SIGSEGV"
        And the exception "message" equals "Segmentation violation (invalid memory reference)"
        And the exception "type" equals "c"
        And the event "severity" equals "error"
        And the event "unhandled" is true

    Scenario: Improper object type cast
        When I run "CXXImproperTypecastScenario" and relaunch the app
        And I configure the app to run in the "non-crashy" state
        And I configure Bugsnag for "CXXImproperTypecastScenario"
        And I wait to receive a request
        And the exception "errorClass" equals "SIGSEGV"
        And the exception "message" equals "Segmentation violation (invalid memory reference)"
        And the exception "type" equals "c"
        And the event "severity" equals "error"
        And the event "unhandled" is true

    Scenario: Program abort()
        When I run "CXXAbortScenario" and relaunch the app
        And I configure the app to run in the "non-crashy" state
        And I configure Bugsnag for "CXXAbortScenario"
        And I wait to receive a request
        And the request payload contains a completed unhandled native report
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
        When I run "CXXSigillScenario" and relaunch the app
        And I configure the app to run in the "non-crashy" state
        And I configure Bugsnag for "CXXSigillScenario"
        And I wait to receive a request
        And the request payload contains a completed unhandled native report
        And the exception "errorClass" equals "SIGILL"
        And the exception "message" equals one of:
            | Illegal instruction   |
            | Trace/breakpoint trap |
        And the exception "type" equals "c"
        And the event "severity" equals "error"
        And the event "unhandled" is true

    Scenario: Raise SIGSEGV
        When I run "CXXSigsegvScenario" and relaunch the app
        And I configure the app to run in the "non-crashy" state
        And I configure Bugsnag for "CXXSigsegvScenario"
        And I wait to receive a request
        And the request payload contains a completed unhandled native report
        And the exception "errorClass" equals "SIGSEGV"
        And the exception "message" equals "Segmentation violation (invalid memory reference)"
        And the exception "type" equals "c"
        And the event "severity" equals "error"
        And the event "unhandled" is true

    Scenario: Raise SIGABRT
        When I run "CXXSigabrtScenario" and relaunch the app
        And I configure the app to run in the "non-crashy" state
        And I configure Bugsnag for "CXXSigabrtScenario"
        And I wait to receive a request
        And the request payload contains a completed unhandled native report
        And the exception "errorClass" equals "SIGABRT"
        And the exception "message" equals "Abort program"
        And the exception "type" equals "c"
        And the event "severity" equals "error"
        And the event "unhandled" is true

    Scenario: Raise SIGBUS
        When I run "CXXSigbusScenario" and relaunch the app
        And I configure the app to run in the "non-crashy" state
        And I configure Bugsnag for "CXXSigbusScenario"
        And I wait to receive a request
        And the request payload contains a completed unhandled native report
        And the exception "errorClass" equals "SIGBUS"
        And the exception "message" equals "Bus error (bad memory access)"
        And the exception "type" equals "c"
        And the event "severity" equals "error"
        And the event "unhandled" is true

    Scenario: Raise SIGFPE
        When I run "CXXSigfpeScenario" and relaunch the app
        And I configure the app to run in the "non-crashy" state
        And I configure Bugsnag for "CXXSigfpeScenario"
        And I wait to receive a request
        And the request payload contains a completed unhandled native report
        And the exception "errorClass" equals "SIGFPE"
        And the exception "message" equals "Floating-point exception"
        And the exception "type" equals "c"
        And the event "severity" equals "error"
        And the event "unhandled" is true

    Scenario: Raise SIGTRAP
        When I run "CXXSigtrapScenario" and relaunch the app
        And I configure the app to run in the "non-crashy" state
        And I configure Bugsnag for "CXXSigtrapScenario"
        And I wait to receive a request
        And the request payload contains a completed unhandled native report
        And the exception "errorClass" equals "SIGTRAP"
        And the exception "message" equals "Trace/breakpoint trap"
        And the exception "type" equals "c"
        And the event "severity" equals "error"
        And the event "unhandled" is true

    Scenario: Undefined JNI method
        When I run "UnsatisfiedLinkErrorScenario" and relaunch the app
        And I configure the app to run in the "non-crashy" state
        And I configure Bugsnag for "UnsatisfiedLinkErrorScenario"
        And I wait to receive a request
        And the report contains the required fields
        And the exception "errorClass" equals "java.lang.UnsatisfiedLinkError"
        And the exception "type" equals "android"
        And the event "severity" equals "error"
        And the event "unhandled" is true

    Scenario: Causing a crash in a separate library
        When I run "CXXExternalStackElementScenario" and relaunch the app
        And I configure the app to run in the "non-crashy" state
        And I configure Bugsnag for "CXXExternalStackElementScenario"
        And I wait to receive a request
        And the request payload contains a completed unhandled native report
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
            | something_innocuous | | libmonochrome.so |
            | Java_com_bugsnag_android_mazerunner_scenarios_CXXExternalStackElementScenario_crash | | libentrypoint.so |

    Scenario: Throwing an exception in C++
        When I run "CXXExceptionScenario" and relaunch the app
        And I configure the app to run in the "non-crashy" state
        And I configure Bugsnag for "CXXExceptionScenario"
        And I wait to receive a request
        And the request payload contains a completed unhandled native report
        And the event "severity" equals "error"
        And the event "unhandled" is true
        And the exception "errorClass" equals "PSt13runtime_error"
        And the exception "message" equals "How about NO"
        And the first significant stack frame methods and files should match:
            | run_away(bool)       | | libentrypoint.so |
            | trigger_an_exception | | libentrypoint.so |
            | Java_com_bugsnag_android_mazerunner_scenarios_CXXExceptionScenario_crash | | libentrypoint.so |

    Scenario: Throwing an object in C++
        When I run "CXXThrowSomethingScenario" and relaunch the app
        And I configure the app to run in the "non-crashy" state
        And I configure Bugsnag for "CXXThrowSomethingScenario"
        And I wait to receive a request
        And the request payload contains a completed unhandled native report
        And the event "severity" equals "error"
        And the event "unhandled" is true
        And the exception "errorClass" equals "i"
        And the exception "message" equals "42"
        And the first significant stack frame methods and files should match:
            | run_back(int, int) | crash_abort | libentrypoint.so |
            | throw_an_object    | | libentrypoint.so |
            | Java_com_bugsnag_android_mazerunner_scenarios_CXXThrowSomethingScenario_crash | | libentrypoint.so |
