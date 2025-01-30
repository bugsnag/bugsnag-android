Feature: Raising native signals

  Background:
    Given I clear all persistent data

  Scenario: Raise SIGILL
    When I run "CXXSigillScenario" and relaunch the crashed app
    And I configure Bugsnag for "CXXSigillScenario"
    And I wait to receive an error
    And the error payload contains a completed unhandled native report
    And the exception "errorClass" equals "SIGILL"
    And the exception "message" equals one of:
      | Illegal instruction                      |
      | Trace/breakpoint trap                    |
      | Illegal instruction, code 4 (ILL_ILLTRP)     |
      | Trace/breakpoint trap, code 5 (TRAP_UNK) |
    And the exception "type" equals "c"
    And the event "severity" equals "error"
    And the event "unhandled" is true

  Scenario: Raise SIGSEGV
    When I run "CXXSigsegvScenario" and relaunch the crashed app
    And I configure Bugsnag for "CXXSigsegvScenario"
    And I wait to receive an error
    And the error payload contains a completed unhandled native report
    And the exception "errorClass" equals "SIGSEGV"
    And the exception "message" equals "Segmentation violation (invalid memory reference)"
    And the exception "type" equals "c"
    And the event "severity" equals "error"
    And the event "unhandled" is true

  Scenario: Raise SIGABRT
    When I run "CXXSigabrtScenario" and relaunch the crashed app
    And I configure Bugsnag for "CXXSigabrtScenario"
    And I wait to receive an error
    And the error payload contains a completed unhandled native report
    And the exception "errorClass" equals "SIGABRT"
    And the exception "message" equals "Abort program"
    And the exception "type" equals "c"
    And the event "severity" equals "error"
    And the event "unhandled" is true

  Scenario: Raise SIGBUS
    When I run "CXXSigbusScenario" and relaunch the crashed app
    And I configure Bugsnag for "CXXSigbusScenario"
    And I wait to receive an error
    And the error payload contains a completed unhandled native report
    And the exception "errorClass" equals "SIGBUS"
    And the exception "message" equals "Bus error (bad memory access)"
    And the exception "type" equals "c"
    And the event "severity" equals "error"
    And the event "unhandled" is true

  Scenario: Raise SIGFPE
    When I run "CXXSigfpeScenario" and relaunch the crashed app
    And I configure Bugsnag for "CXXSigfpeScenario"
    And I wait to receive an error
    And the error payload contains a completed unhandled native report
    And the exception "errorClass" equals "SIGFPE"
    And the exception "message" equals one of:
      | Floating-point exception                  |
      | Floating-point exception, code 8 (FPE_FLTSUB) |
    And the exception "type" equals "c"
    And the event "severity" equals "error"
    And the event "unhandled" is true

  Scenario: Raise SIGTRAP
    When I run "CXXSigtrapScenario" and relaunch the crashed app
    And I configure Bugsnag for "CXXSigtrapScenario"
    And I wait to receive an error
    And the error payload contains a completed unhandled native report
    And the exception "errorClass" equals "SIGTRAP"
    And the exception "message" equals one of:
      | Trace/breakpoint trap                    |
      | Trace/breakpoint trap, code 5 (TRAP_UNK) |
    And the exception "type" equals "c"
    And the event "severity" equals "error"
    And the event "unhandled" is true
