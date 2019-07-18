# bugsnag-plugin-android-anr

This module detects ANRs and reports them to bugsnag.

## High-level Overview

When an ANR dialog is shown SIGQUIT is raised. This module installs a SIGQUIT handler and sets a
ByteBuffer that is continuously monitored from the JVM. When the JVM code detects a ByteBuffer has
been modified, it generates a report of the ANR.
