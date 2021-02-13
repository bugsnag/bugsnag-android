Feature: Native crash reporting

Scenario: Dereference a null pointer
        When I run "CXXNullPointerScenario" and relaunch the app
        And I configure Bugsnag for "CXXNullPointerScenario"
        And I wait to receive an error
        And the error payload contains a completed unhandled native report
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
        And the error payload field "events.0.device.cpuAbi" is a non-empty array

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
        And I configure Bugsnag for "CXXStackoverflowScenario"
        And I wait to receive an error
        And the error payload contains a completed unhandled native report
        And the exception reflects a signal was raised
        And the exception "type" equals "c"
        And the event "severity" equals "error"
        And the event "unhandled" is true

    Scenario: Program trap()
        When I run "CXXTrapScenario" and relaunch the app
        And I configure Bugsnag for "CXXTrapScenario"
        And I wait to receive an error
        And the error payload contains a completed unhandled native report
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
        And I configure Bugsnag for "CXXWriteReadOnlyMemoryScenario"
        And I wait to receive an error
        And the exception "errorClass" equals "SIGSEGV"
        And the exception "message" equals "Segmentation violation (invalid memory reference)"
        And the exception "type" equals "c"
        And the event "severity" equals "error"
        And the event "unhandled" is true

    # Skip due to an issue on later Android platforms - [PLAT-5465]
    @skip_android_10 @skip_android_11
    Scenario: Double free() allocated memory
        When I run "CXXDoubleFreeScenario" and relaunch the app
        And I configure Bugsnag for "CXXDoubleFreeScenario"
        And I wait to receive an error
        And the exception "type" equals "c"
        And the event "severity" equals "error"
        And the event "unhandled" is true
        # Fix as part of PLAT-5643
        # And the exception "errorClass" equals "SIGSEGV"
        # And the exception "message" equals "Segmentation violation (invalid memory reference)"

    Scenario: Improper object type cast
        When I run "CXXImproperTypecastScenario" and relaunch the app
        And I configure Bugsnag for "CXXImproperTypecastScenario"
        And I wait to receive an error
        And the exception "errorClass" equals "SIGSEGV"
        And the exception "message" equals "Segmentation violation (invalid memory reference)"
        And the exception "type" equals "c"
        And the event "severity" equals "error"
        And the event "unhandled" is true

    Scenario: Program abort()
        When I run "CXXAbortScenario" and relaunch the app
        And I configure Bugsnag for "CXXAbortScenario"
        And I wait to receive an error
        And the error payload contains a completed unhandled native report
        And the exception "errorClass" equals one of:
            | SIGABRT |
            | SIGSEGV |
        And the exception "message" equals one of:
            | Abort program |
            | Segmentation violation (invalid memory reference) |
        And the exception "type" equals "c"
        And the event "severity" equals "error"
        And the event "unhandled" is true

    Scenario: Undefined JNI method
        When I run "UnsatisfiedLinkErrorScenario" and relaunch the app
        And I configure Bugsnag for "UnsatisfiedLinkErrorScenario"
        And I wait to receive an error
        And the report contains the required fields
        And the exception "errorClass" equals "java.lang.UnsatisfiedLinkError"
        And the exception "type" equals "android"
        And the event "severity" equals "error"
        And the event "unhandled" is true

    Scenario: Causing a crash in a separate library
        When I run "CXXExternalStackElementScenario" and relaunch the app
        And I configure Bugsnag for "CXXExternalStackElementScenario"
        And I wait to receive an error
        And the error payload contains a completed unhandled native report
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
            | Java_com_bugsnag_android_mazerunner_scenarios_CXXExternalStackElementScenario_crash | | libcxx-scenarios.so |
