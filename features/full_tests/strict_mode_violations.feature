Feature: Reporting Strict Mode Violations

  Background:
    Given I clear all persistent data

  # TODO: Skipped pending PLAT-9675
  @skip
  @skip_below_android_9
  Scenario: StrictMode Exposed File URI violation
    When I run "StrictModeFileUriExposeScenario" and relaunch the crashed app
    And I configure Bugsnag for "StrictModeFileUriExposeScenario"
    And I wait to receive an error
    Then the error is valid for the error reporting API version "4.0" for the "Android Bugsnag Notifier" notifier
    And the exception "errorClass" equals "android.os.strictmode.FileUriExposedViolation"
    And the exception "message" equals "StrictMode policy violation detected: VmPolicy"
    And the event "unhandled" is false
    And the event "severity" equals "info"
    And the event "severityReason.type" equals "strictMode"
    And the error payload field "events.0.exceptions.0.stacktrace" is a non-empty array
    And the event "exceptions.0.stacktrace.0.method" equals "android.os.StrictMode.onFileUriExposed"
    And the event "exceptions.0.stacktrace.0.file" equals "StrictMode.java"

  @skip_below_android_9
  Scenario: StrictMode DiscWrite violation
    When I run "StrictModeDiscScenario" and relaunch the crashed app
    And I configure Bugsnag for "StrictModeDiscScenario"
    And I wait to receive 2 errors

    # First violation (triggered by opening FileOutputStream)
    Then the error is valid for the error reporting API version "4.0" for the "Android Bugsnag Notifier" notifier
    And the exception "errorClass" equals "android.os.strictmode.DiskWriteViolation"
    And the exception "message" equals "StrictMode policy violation detected: ThreadPolicy"
    And the event "unhandled" is false
    And the event "severity" equals "info"
    And the event "severityReason.type" equals "strictMode"
    And the error payload field "events.0.exceptions.0.stacktrace" is a non-empty array

    And the exception "stacktrace.1.method" equals one of:
      | libcore.io.BlockGuardOs.open    |
      | java.io.FileOutputStream.<init> |

    And the exception "stacktrace.1.file" equals one of:
      | BlockGuardOs.java     |
      | FileOutputStream.java |

    # Second violation (triggered by writing to FileOutputStream)
    And I discard the oldest error
    Then the error is valid for the error reporting API version "4.0" for the "Android Bugsnag Notifier" notifier
    And the exception "errorClass" equals "android.os.strictmode.DiskWriteViolation"
    And the exception "message" equals "StrictMode policy violation detected: ThreadPolicy"
    And the event "unhandled" is false
    And the event "severity" equals "info"
    And the event "severityReason.type" equals "strictMode"
    And the error payload field "events.0.exceptions.0.stacktrace" is a non-empty array
    And the event "exceptions.0.stacktrace.1.method" equals "libcore.io.BlockGuardOs.write"
    And the event "exceptions.0.stacktrace.1.file" equals "BlockGuardOs.java"
