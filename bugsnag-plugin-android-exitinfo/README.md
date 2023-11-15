# bugsnag-plugin-android-exitinfo

This module enhances your crash reports on Android 11+ devices by merging data captured in
the [ApplicationExitInfo](https://developer.android.com/reference/android/app/ApplicationExitInfo).

## High-level Overview

This module correlates `ApplicationExitInfo` traces and tombstones with reports being sent by
bugsnag-android-core, and augments the existing error reports with information extracted from
these files (such as the full stack-traces from all threads in native crashes).