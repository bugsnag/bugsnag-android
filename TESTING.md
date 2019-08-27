# Testing the Bugsnag Android notifier

Commands can be run on the entire project, or on an individual module:

```shell
./gradlew build // builds whole project
./gradlew bugsnag-plugin-android-anr:build // builds bugsnag-plugin-android-anr module only
```

## Static analysis

```shell
./gradlew lint checkstyle detekt
```

## Running Tests Locally

Running the full test suite requires a connected android device or emulator. JVM tests can be run
in isolation by only running the `check` task.

```shell
./gradlew check connectedCheck
```

## End-to-end tests

To run the end-to-end tests, first set up the environment by running
[Bundler](https://bundler.io):

```shell
bundle install
```

The tests require two environment variables to be set:

* `ANDROID_HOME`, set the the location of the Android SDK
* `ANDROID_EMULATOR`, set to the name of an installed emulator

Then run the tests using:

```shell
bundle exec maze-runner
```

## Running remote tests

These tests are implemented with our notifier testing tool [Maze runner](https://github.com/bugsnag/maze-runner).

End to end tests are written in cucumber-style `.feature` files, and need Ruby-backed "steps" in order to know what to run. The tests are located in the top level [`tests`](/tests/) directory.

Maze runner's CLI and the test fixtures are containerised so you'll need Docker (and Docker Compose) to run them.

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

* `BROWSER_STACK_USERNAME`: The BrowserStack App Automate Username
* `BROWSER_STACK_ACCESS_KEY`: The BrowserStack App Automate Access Key
* `DEVICE_TYPE`: The android version to run the tests against, one of: ANDROID_5, ANDROID_6, ANDROID_7, ANDROID_8, ANDROID_9

Run `make remote-integration-tests`

If you wish to test a single feature, set the `TEST_FEATURE` environment variable to the name of the feature file.
For example, to test the `breadcrumb` feature, use the following command:

`TEST_FEATURE=breadcrumb.feature make remote-integration-tests`
