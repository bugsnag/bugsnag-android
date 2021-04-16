Developing bugsnag-android
============

This provides an overview of common tasks which you are likely to perform in development, and assumes that you have performed the pre-requisite [environment setup](ENVIRONMENT_SETUP.md)

## Compiling the app

`./gradlew assembleRelease` will compile a release build of the library.

## Running unit tests

`./gradlew test` will run JVM unit tests, and `./gradlew check` will additionally run static analysis checks. See the [testing docs](TESTING.md) for further details.

## Running E2E tests

Please see the [mazerunner docs](MAZERUNNER.md) for information on how to run E2E tests locally.

## Example app

The example app can be found in [examples/sdk-app-example](../examples/sdk-app-example). It contains example code which triggers crashes.

You should open `examples/sdk-app-example` as a separate Android Studio project and [run the app](https://developer.android.com/training/basics/firstapp/running-app). This will require a connected device or emulator.

The app **does not use the local development version of Bugsnag**. To test local development changes, you will need to build the library locally with an arbitrary version number, e.g. `9.9.9`, and update the `bugsnag-android` dependency in `examples/sdk-app-example/build.gradle` to use that version.

### Building the Library locally

The following command assembles bugsnag-android for local use:

```shell
./gradlew assembleRelease publishToMavenLocal -PVERSION_NAME=9.9.9
```

This installs bugsnag-android to a local maven repository at `~/.m2/repository/com/bugsnag/`. The example app should automatically use
this version when you next run the app.

## Building with custom ABIs

By default, the NDK module will be built with the following ABIs:

- arm64-v8a
- armeabi-v7a
- x86
- x86_64

To build the NDK module with specific ABIs, use the `ABI_FILTERS` project
option:

```shell
./gradlew assembleRelease -PABI_FILTERS=x86,arm64-v8a
```
