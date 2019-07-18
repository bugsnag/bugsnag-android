# bugsnag-plugin-android-ndk

This module detects NDK signals/exceptions and reports them to bugsnag.

## High-level Overview

This module installs C signal handlers and a CPP exception handler. When a native crash occurs,
it writes a report to disk. This is then converted to a JVM report on the next app launch, and is
delivered to the error reporting API.
