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

## Running remote end-to-end tests

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
