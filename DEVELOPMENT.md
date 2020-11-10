Developing bugsnag-android
============

The following guide should get you setup for developing bugsnag-android.

# Initial setup

1. Download the [latest stable version](https://developer.android.com/studio) of Android Studio.
2. Download [r16b of the NDK](https://developer.android.com/ndk/downloads/older_releases)
3. Set the `$ANDROID_HOME` and `$ANDROID_NDK_HOME` environment variables to point to the SDKs, and optionally add the `adb/android/emulator` [platform tools](https://developer.android.com/studio/command-line/variables) to your `$PATH`
4. Clone the repository and its submodules: `git submodule update --init --recursive`
5. Open the project in Android Studio to trigger indexing and downloading of dependencies

# Project structure

The project is comprised of three [Gradle modules](https://docs.gradle.org/current/userguide/multi_project_builds.html):

- [bugsnag-android-core](bugsnag-android-core/README.md), responsible for capturing JVM errors and delivery of payloads
- [bugsnag-plugin-android-ndk](bugsnag-plugin-android-ndk/README.md), responsible for capturing NDK errors
- [bugsnag-plugin-android-anr](bugsnag-plugin-android-anr/README.md), responsible for capturing ANRs

[Gradle tasks](https://docs.gradle.org/current/userguide/tutorial_using_tasks.html) can generally be run on the entire project, or on an individual module: 

```shell
./gradlew build // builds whole project
./gradlew bugsnag-plugin-android-anr:build // builds one module only
```

# Static analysis

Several static analysis checks are run against bugsnag-android to maintain the quality of the codebase. `./gradlew lint ktlintCheck detekt checkstyle` runs them all at once.

## Android Lint

[Android Lint](https://developer.android.com/studio/write/lint) Runs Android-specific static analysis checks. Warnings can be suppressed by following [this guide](https://developer.android.com/studio/write/lint#config).

```shell
./gradlew lint
```

## Ktlint

[Ktlint](https://github.com/pinterest/ktlint) runs style checks on Kotlin code.

```shell
./gradlew ktlintCheck
```

Some violations (but not all) can be automatically addressed by running the following task:

```shell
./gradlew ktlintFormat
```

## Detekt

[Detekt](https://github.com/detekt/detekt) runs static analysis checks on Kotlin code.

```shell
./gradlew detekt
```

Warnings can be suppressed by running the following task:

```shell
./gradlew detektBaseline
```

This permanently disables all outstanding violations by writing to `detekt-baseline.xml`.

## Checkstyle

[Checkstyle](https://github.com/checkstyle/checkstyle) runs style checks on Java code.

```shell
./gradlew checkstyle
```

# Unit tests

Unit tests are implemented using [Junit](https://developer.android.com/training/testing/unit-testing/local-unit-tests) and can be run with the following:

`./gradlew test`

Unit tests run on the local JVM and cannot access Android OS classes.

# Instrumentation tests

Instrumentation tests are implemented using [Junit](https://developer.android.com/training/testing/unit-testing/instrumented-unit-tests) and can be run with the following:

`./gradlew connectedCheck`

Instrumentation tests require an Android emulator or device to run, and they can access Android OS classes.

# Example app

The example app can be found in [examples/sdk-app-example](examples/sdk-app-example). It contains example code which triggers crashes.

You should open `examples/sdk-app-example` as a separate Android Studio project and [run the app](https://developer.android.com/training/basics/firstapp/running-app). This will require a connected device or emulator.

The app **does not use the local development version of Bugsnag**. To test local development changes, you will need to build the library locally with a high version number, and update the `bugsnag-android` dependency in `examples/sdk-app-example/build.gradle` to use that version.

## Building the Library locally

The following command assembles bugsnag-android for local use:

```shell
./gradlew assembleRelease publishToMavenLocal -PVERSION_NAME=9.9.9
```

This installs bugsnag-android to a local maven repository at `~/.m2/repository/com/bugsnag/`. The example app should automatically use
this version when you next run the app.

# Running remote end-to-end tests

These tests are implemented with our notifier testing tool [Maze runner](https://github.com/bugsnag/maze-runner).

End to end tests are written in cucumber-style `.feature` files, and need Ruby-backed "steps" in order to know what to run. The tests are located in the top level [`features`](/features/) directory.

Maze Runner's CLI and the test fixtures are containerized so you'll need Docker (and Docker Compose) to run them.

__Note: only Bugsnag employees can run the end-to-end tests.__ We have dedicated test infrastructure and private BrowserStack credentials which can't be shared outside of the organisation.

##### Authenticating with the private container registry

You'll need to set the credentials for the aws profile in order to access the private docker registry:

```
aws configure --profile=opensource
```

Subsequently you'll need to run the following commmand to authenticate with the registry:

```
$(aws ecr get-login --profile=opensource --no-include-email)
```

__Your session will periodically expire__, so you'll need to run this command to re-authenticate when that happens.

Remote tests can be run against real devices provided by BrowserStack. In order to run these tests, you need:

A BrowserStack App Automate Username: `BROWSER_STACK_USERNAME`
A BrowserStack App Automate Access Key: `BROWSER_STACK_ACCESS_KEY`
A local docker and docker-compose installation.

### Instrumentation tests

Ensure that the following environment variables are set:

* `BROWSER_STACK_USERNAME`: The BrowserStack App Automate Username
* `BROWSER_STACK_ACCESS_KEY`: The BrowserStack App Automate Access Key
* `NDK_VERSION`: The version of NDK that should be used to build the app

Run `make remote-test`

### End-to-end tests

Ensure that the following environment variables are set:
* `BROWSER_STACK_USERNAME`: Your BrowserStack App Automate Username
* `BROWSER_STACK_ACCESS_KEY`: You BrowserStack App Automate Access Key

See https://www.browserstack.com/local-testing/app-automate for details of the required local testing binary.

1. Build the test fixture `make test-fixture`
1. Check the contents of `Gemfile` to select the version of `maze-runner` to use
1. To run a single feature:
    ```shell script
    bundle exec maze-runner --app=build/fixture.apk                 \
                            --farm=bs                               \
                            --device=ANDROID_9_0                    \
                            --username=$BROWSER_STACK_USERNAME      \
                            --access-key=$BROWSER_STACK_ACCESS_KEY  \
                            --bs-local=~/BrowserStackLocal          \
                            features/app_version.feature
    ```
1. To run all features, omit the final argument.
1. Maze Runner also supports all options that Cucumber does.  Run `bundle exec maze-runner --help` for full details.

# Building with custom ABIs

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

For release purposes, the Makefile's build command includes the "armeabi" ABI for compatibility with devices using r16 and below of the NDK.

# Releasing a New Version

Full details of how to release can be found in [the release guide](RELEASING.md)

