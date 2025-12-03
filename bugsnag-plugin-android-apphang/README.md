# bugsnag-plugin-android-apphang

An alternative to Application Not Responding (ANR) reporting with configurable timeouts.

## High-level Overview

AppHangs can be considered an alternative to ANR reporting. While
[bugsnag-plugin-android-anr](../bugsnag-plugin-android-anr) reports are based on the system
ANR signal, the AppHang plugin can be configured to a specific threshold.

AppHangs are only detected while the app is in the foreground and background detection is not
currently supported.

## ANR and AppHang Reporting

AppHang reporting can be combined with ANR reporting safely, both signals will be detected and
reported. AppHang error reports will typically be visible as breadcrumbs in ANR reports in these
configurations.
