Feature: Native crash reporting with thrown objects

  Background:
    Given I clear all persistent data

  Scenario: Throwing an exception in C++
    When I run "CXXExceptionScenario" and relaunch the crashed app
    And I configure Bugsnag for "CXXExceptionScenario"
    And I wait to receive an error
    And the error payload contains a completed unhandled native report
    And the event "severity" equals "error"
    And the event "unhandled" is true
    And the exception "errorClass" equals "SIGABRT"
    And the exception "message" equals "Abort program"

  Scenario: Throwing an object in C++
    When I run "CXXThrowSomethingScenario" and relaunch the crashed app
    And I configure Bugsnag for "CXXThrowSomethingScenario"
    And I wait to receive an error
    And the error payload contains a completed unhandled native report
    And the event "severity" equals "error"
    And the event "unhandled" is true
    And the exception "errorClass" equals "SIGABRT"
    And the exception "message" equals "Abort program"

  Scenario: Rethrow in C++ without initial exception
    When I run "CXXInvalidRethrow" and relaunch the crashed app
    And I configure Bugsnag for "CXXInvalidRethrow"
    And I wait to receive an error
    And the error payload contains a completed unhandled native report
    And the exception "errorClass" equals "SIGABRT"
    And the exception "type" equals "c"
    And the event "severity" equals "error"
    And the event "unhandled" is true
    And the first significant stack frames match:
      | print_last_exception() | CXXInvalidRethrow.cpp | 7 |

  Scenario: Throw from C++ noexcept function
    When I run "CXXThrowFromNoexcept" and relaunch the crashed app
    And I configure Bugsnag for "CXXThrowFromNoexcept"
    And I wait to receive an error
    And the error payload contains a completed unhandled native report
    And the exception "errorClass" equals "SIGABRT"
    And the exception "type" equals "c"
    And the event "severity" equals "error"
    And the event "unhandled" is true
    And on arm64, the first significant stack frames match:
      | FunkError::what() const                                                  | CXXThrowFromNoexcept.cpp | 13 |
      | Java_com_bugsnag_android_mazerunner_scenarios_CXXThrowFromNoexcept_crash | CXXThrowFromNoexcept.cpp | 21 |

