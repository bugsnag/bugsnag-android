# bugsnag-android-core

This module detects JVM exceptions and reports them to bugsnag.

## High-level Overview

An `UncaughtExceptionHandler` is installed which generates an error report whenever an uncaught 
`Throwable` propagates. This module is also responsible for the delivery of JSON payloads to the
Error Reporting API, and also provides the main public API for bugsnag-android.
