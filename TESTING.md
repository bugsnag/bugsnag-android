
# Unit tests

Unit tests are implemented using [Junit](https://developer.android.com/training/testing/unit-testing/local-unit-tests) and can be run with the following:

`./gradlew test`

Unit tests run on the local JVM and cannot access Android OS classes.

# Instrumentation tests

Instrumentation tests are implemented using [Junit](https://developer.android.com/training/testing/unit-testing/instrumented-unit-tests) and can be run with the following:

`./gradlew connectedCheck`

Instrumentation tests require an Android emulator or device to run, and they can access Android OS classes.

# Static analysis

Several static analysis checks are run against bugsnag-android to maintain the quality of the codebase. `./gradlew check` runs them all at once.

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

A BrowserStack App Automate Username: `MAZE_DEVICE_FARM_USERNAME`
A BrowserStack App Automate Access Key: `MAZE_DEVICE_FARM_ACCESS_KEY`
A local docker and docker-compose installation.

### Instrumentation tests

Ensure that the following environment variables are set:

* `MAZE_DEVICE_FARM_USERNAME`: The BrowserStack App Automate Username
* `MAZE_DEVICE_FARM_ACCESS_KEY`: The BrowserStack App Automate Access Key

Run `make remote-test`

### End-to-end tests

Ensure that the following environment variables are set:
* `MAZE_DEVICE_FARM_USERNAME`: Your BrowserStack App Automate Username
* `MAZE_DEVICE_FARM_ACCESS_KEY`: You BrowserStack App Automate Access Key
* `MAZE_BS_LOCAL`: Location of the BrowserStack local testing binary (see https://www.browserstack.com/local-testing/app-automate).

1. Build the test fixture `make test-fixture`
1. Check the contents of `Gemfile` to select the version of `maze-runner` to use
1. To run a single feature:
    ```shell script
    make test-fixture && \
    bundle exec maze-runner --app=build/fixture.apk                 \
                            --farm=bs                               \
                            --device=ANDROID_9_0                    \
                            features/smoke_tests/unhandled.feature
    ```
1. To run all features, omit the final argument.
1. Maze Runner also supports all options that Cucumber does.  Run `bundle exec maze-runner --help` for full details.
