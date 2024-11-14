# Project structure

This gives a high-level overview of the major components of the bugsnag-android project. Most of these are documented in further detail on specific pages of the docs.

## Modules

The project consists of [6 Gradle modules](https://gradle.org/), which are linked to below:

- [bugsnag-android-core](../bugsnag-android-core/README.md) - contains the core functionality required to capture and deliver JVM errors to Bugsnag.
- [bugsnag-plugin-android-ndk](../bugsnag-plugin-android-ndk/README.md) - contains optional functionality that installs signal handlers and captures NDK errors.
- [bugsnag-plugin-android-anr](../bugsnag-plugin-android-ndk/README.md) - contains optional functionality that installs a signal handler to capture ANR errors.
- [bugsnag-plugin-android-okhttp](../bugsnag-plugin-android-okhttp/README.md) - contains optional functionality for OkHttp.
- [bugsnag-plugin-android-exitinfo](../bugsnag-plugin-android-exitinfo) - contains optional functionality to collect additional data using [historical process exit reasons](https://developer.android.com/reference/android/app/ActivityManager#getHistoricalProcessExitReasons(java.lang.String,%20int,%20int)).
- [bugsnag-plugin-react-native](../bugsnag-plugin-react-native/README.md) - contains optional functionality that serializes information into a format that can be understood by the React Native bridge.
- [bugsnag-android](../bugsnag-android/README.md) - an anchor package which allows users to pull in all the required modules.
- [bugsnag-android-ndk](../bugsnag-android-ndk/README.md) - an anchor package which allows users to pull in all the required modules. Published for legacy reasons.

## Example app

An [example app](../examples/sdk-app-example/README.md) is provided which allows for customers (and Bugsnag maintainers) to test Bugsnag's functionality against crashes. It is an independent gradle project which is stored in the same repository.

## Dockerfiles

The project is containerized with [dockerfiles](../dockerfiles). These are used to test the project on CI with [Buildkite](http://buildkite.com/).

## Git submodule

The project uses one git submodule to access [libunwindstack](https://github.com/bugsnag/libunwindstack-ndk), which is used for capturing NDK stacktraces.

## E2E tests

Bugsnag makes extensive use of E2E testing with [mazerunner](https://github.com/bugsnag/maze-runner), which is our custom black-box testing framework written in Ruby.

The [features](../features) directory contains test fixtures that run crashy code against tests written in [cucumber](https://cucumber.io/). These verify whether Bugsnag captures appropriate information in crashy scenarios.
