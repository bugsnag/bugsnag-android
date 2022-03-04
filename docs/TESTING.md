# Testing bugsnag-android

This documents code quality checks that are used to improve the quality of bugsnag-android. Most of these can be run all together using `./gradlew check`.

## Unit tests

Unit tests are implemented using [Junit](https://developer.android.com/training/testing/unit-testing/local-unit-tests) and can be run with the following:

`./gradlew test`

Unit tests run on the local JVM and cannot access Android OS classes.

## Instrumentation tests

Instrumentation tests are implemented using [Junit](https://developer.android.com/training/testing/unit-testing/instrumented-unit-tests) and can be run with the following:

`./gradlew connectedCheck`

Instrumentation tests require an Android emulator or device to run, and they can access Android OS classes.

## Static analysis

Several static analysis and code style checks are run against bugsnag-android to maintain the quality of the codebase. `make check` runs them all at once.

### Android Lint

[Android Lint](https://developer.android.com/studio/write/lint) Runs Android-specific static analysis checks. Warnings can be suppressed by following [this guide](https://developer.android.com/studio/write/lint#config).

```shell
./gradlew lint
```

### Ktlint

[Ktlint](https://github.com/pinterest/ktlint) runs style checks on Kotlin code.

```shell
./gradlew ktlintCheck
```

Some violations (but not all) can be automatically addressed by running the following task:

```shell
./gradlew ktlintFormat
```

### Detekt

[Detekt](https://github.com/detekt/detekt) runs static analysis checks on Kotlin code.

```shell
./gradlew detekt
```

Warnings can be suppressed by running the following task:

```shell
./gradlew detektBaseline
```

This permanently disables all outstanding violations by writing to `detekt-baseline.xml`.

### Checkstyle

[Checkstyle](https://github.com/checkstyle/checkstyle) runs style checks on Java code.

```shell
./gradlew checkstyle
```

### CPP Check

[CPP check](http://cppcheck.sourceforge.net/) runs code analysis on C++ code to detect bugs.

```shell
./scripts/run-cpp-check.sh
```

### ClangFormat

[ClangFormat](https://clang.llvm.org/docs/ClangFormat.html) formats C++ code to ensure a consistent code style.

```shell
./scripts/run-clang-format.sh
```

### GWP-ASAN

[GWP-ASAN](https://developer.android.com/ndk/guides/gwp-asan) is an address sanitizer tool which is enabled on our E2E tests that target Android 11+. It randomly samples memory and aborts the process if memory was misused.

## Build scan

[Gradle build scans](https://scans.gradle.com/) are enabled on CI so that the project's build performance can be benchmarked when necessary.

## Size reporting

The size impact of the bugsnag-android SDK is reported onto Github pull requests using this [Dangerfile](../features/fixtures/minimalapp/Dangerfile).

## Running remote end-to-end tests

Please see the [mazerunner docs](MAZERUNNER.md) for information on how to run E2E tests locally.
