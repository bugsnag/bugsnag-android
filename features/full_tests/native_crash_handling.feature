Feature: Native crash reporting

  Background:
    Given I clear all persistent data

  Scenario: Dereference a null pointer
    When I run "CXXDereferenceNullScenario" and relaunch the crashed app
    And I configure Bugsnag for "CXXDereferenceNullScenario"
    And I wait to receive an error
    And the error payload contains a completed unhandled native report
    And the exception "errorClass" equals "SIGSEGV"
    And the exception "message" equals "Segmentation violation (invalid memory reference)"
    And the exception "type" equals "c"
    And the event "severity" equals "error"
    And the event "unhandled" is true
    And the event "app.binaryArch" is not null
    And the event "device.totalMemory" is not null
    And the error payload field "events.0.device.cpuAbi" is a non-empty array
    And the error payload field "events.0.metaData.app.memoryLimit" is greater than 0
    And the first significant stack frames match:
      | get_the_null_value() | CXXDereferenceNullScenario.cpp | 7 |
    And the "codeIdentifier" of stack frame 0 is not null

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
    When I run "CXXStackoverflowScenario" and relaunch the crashed app
    And I configure Bugsnag for "CXXStackoverflowScenario"
    And I wait to receive an error
    And the error payload contains a completed unhandled native report
    And the exception reflects a signal was raised
    And the exception "type" equals "c"
    And the event "severity" equals "error"
    And the event "unhandled" is true
    And the first significant stack frames match:
      | crash_stack_overflow | CXXStackoverflowScenario.cpp |
    And the "codeIdentifier" of stack frame 0 is not null

  Scenario: Program trap()
    When I run "CXXTrapScenario" and relaunch the crashed app
    And I configure Bugsnag for "CXXTrapScenario"
    And I wait to receive an error
    And the error payload contains a completed unhandled native report
    And the exception "errorClass" equals one of:
      | SIGILL  |
      | SIGTRAP |
    And the exception "message" equals one of:
      | Illegal instruction                    |
      | Trace/breakpoint trap                  |
      | Illegal instruction, code 4 (ILL_ILLTRP)   |
      | Illegal instruction, code 1 (ILL_ILLOPC)   |
      | Trace/breakpoint trap, code 1 (TRAP_BRKPT)  |
    And the exception "type" equals "c"
    And the event "severity" equals "error"
    And the event "unhandled" is true
    And the first significant stack frames match:
      | trap_it() | CXXTrapScenario.cpp | 12 |
    And the "codeIdentifier" of stack frame 0 is not null

  Scenario: Write to read-only memory
    When I run "CXXWriteReadOnlyMemoryScenario" and relaunch the crashed app
    And I configure Bugsnag for "CXXWriteReadOnlyMemoryScenario"
    And I wait to receive an error
    And the exception "errorClass" equals "SIGSEGV"
    And the exception "message" equals "Segmentation violation (invalid memory reference)"
    And the exception "type" equals "c"
    And the event "severity" equals "error"
    And the event "unhandled" is true
    And the first significant stack frames match:
      | crash_write_read_only_mem(int)                                                     | CXXWriteReadOnlyMemoryScenario.cpp | 12 |
      | Java_com_bugsnag_android_mazerunner_scenarios_CXXWriteReadOnlyMemoryScenario_crash | CXXWriteReadOnlyMemoryScenario.cpp | 22 |
    And the "codeIdentifier" of stack frame 0 is not null

  Scenario: Improper object type cast
    When I run "CXXImproperTypecastScenario" and relaunch the crashed app
    And I configure Bugsnag for "CXXImproperTypecastScenario"
    And I wait to receive an error
    And the exception "errorClass" equals "SIGSEGV"
    And the exception "message" equals "Segmentation violation (invalid memory reference)"
    And the exception "type" equals "c"
    And the event "severity" equals "error"
    And the event "unhandled" is true
    And the first significant stack frames match:
      | crash_improper_cast(void*)                                                      | CXXImproperTypecastScenario.cpp | 12 |
      | Java_com_bugsnag_android_mazerunner_scenarios_CXXImproperTypecastScenario_crash | CXXImproperTypecastScenario.cpp | 20 |
    And the "codeIdentifier" of stack frame 0 is not null

  Scenario: Program abort()
    When I run "CXXAbortScenario" and relaunch the crashed app
    And I configure Bugsnag for "CXXAbortScenario"
    And I wait to receive an error
    And the error payload contains a completed unhandled native report
    And the exception "errorClass" equals one of:
      | SIGABRT |
      | SIGSEGV |
    And the exception "message" equals one of:
      | Abort program                                     |
      | Segmentation violation (invalid memory reference) |
    And the exception "type" equals "c"
    And the event "severity" equals "error"
    And the event "unhandled" is true
    And the first significant stack frames match:
      | evictor::exit_with_style()                                           | CXXAbortScenario.cpp | 5  |
      | Java_com_bugsnag_android_mazerunner_scenarios_CXXAbortScenario_crash | CXXAbortScenario.cpp | 13 |
    And the "codeIdentifier" of stack frame 0 is not null

  Scenario: Undefined JNI method
    When I run "UnsatisfiedLinkErrorScenario" and relaunch the crashed app
    And I configure Bugsnag for "UnsatisfiedLinkErrorScenario"
    And I wait to receive an error
    And the report contains the required fields
    And the exception "errorClass" equals "java.lang.UnsatisfiedLinkError"
    And the exception "type" equals "android"
    And the event "severity" equals "error"
    And the event "unhandled" is true

  # Android 6 dladdr does report .so files that are not extracted from their .apk file
  # this test cannot pass on these devices with extractNativeLibs=false
  @skip_android_6
  Scenario: Causing a crash in a separate library
    When I run "CXXExternalStackElementScenario" and relaunch the crashed app
    And I configure Bugsnag for "CXXExternalStackElementScenario"
    And I wait to receive an error
    And the error payload contains a completed unhandled native report
    And the event "severity" equals "error"
    And the event "unhandled" is true
    And the exception "errorClass" equals one of:
      | SIGILL  |
      | SIGTRAP |
    And the exception "message" equals one of:
      | Illegal instruction                    |
      | Trace/breakpoint trap                  |
      | Illegal instruction, code 1 (ILL_ILLOPC)   |
      | Trace/breakpoint trap, code 1 (TRAP_BRKPT)  |
    And the exception "type" equals "c"
    And the first significant stack frames match:
      | something_innocuous                                                                 | libmonochrome.so                    | (ignore) |
      | Java_com_bugsnag_android_mazerunner_scenarios_CXXExternalStackElementScenario_crash | CXXExternalStackElementScenario.cpp | 20       |
    And the "codeIdentifier" of stack frame 0 is not null

  Scenario: Call null function pointer
  A null pointer should be the first element of a stack trace,
  followed by the calling function

    When I run "CXXCallNullFunctionPointerScenario" and relaunch the crashed app
    And I configure Bugsnag for "CXXCallNullFunctionPointerScenario"
    And I wait to receive an error
    And the error payload contains a completed unhandled native report
    And the exception "errorClass" equals "SIGSEGV"
    And the exception "message" equals "Segmentation violation (invalid memory reference)"
    And the exception "type" equals "c"
    And the event "severity" equals "error"
    And the event "unhandled" is true
    And the "method" of stack frame 0 equals "0x0"
    And the "lineNumber" of stack frame 0 equals 0
    And the first significant stack frames match:
      | dispatch::Handler::handle(_jobject*) | CXXCallNullFunctionPointerScenario.cpp | 9 |

  Scenario: Refresh symbol table during a crash
    When I run "CXXRefreshSymbolTableDuringCrashScenario" and relaunch the crashed app
    And I configure Bugsnag for "CXXRefreshSymbolTableDuringCrashScenario"
    And I wait to receive an error
    Then the error payload contains a completed unhandled native report
    And the exception "errorClass" equals one of:
      | SIGABRT |
      | SIGSEGV |
    And the exception "message" equals one of:
      | Abort program                                     |
      | Segmentation violation (invalid memory reference) |
    And the exception "type" equals "c"
    And the event "severity" equals "error"
    And the event "unhandled" is true
