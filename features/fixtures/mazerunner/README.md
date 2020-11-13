# Mazerunner fixture

The test fixture uses Gradle modules to separate functionality, which makes it possible to assemble multiple fixtures.

## app

Assembles an APK which provides a UI that Appium can use to drive scenarios.

## jvm-scenarios

Library of scenarios that only require the JVM to crash. The `app` module always depends on this.

## cxx-scenarios

Library of scenarios that only require the NDK (without bugsnag) to crash. The `app` module always depends on this.

## cxx-scenarios-bugsnag

Library of scenarios that require the NDK and `bugsnag-plugin-android-ndk` to crash. The `app` module optionally depends on this.
