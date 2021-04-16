# Mazerunner tests

E2E tests are implemented with our notifier testing tool [Maze runner](https://github.com/bugsnag/maze-runner), which is a black-box test framework written in Ruby.

## Running remote end-to-end tests

End to end tests are written in cucumber-style `.feature` files, and need Ruby-backed "steps" in order to know what to run. The tests are located in the top level [`features`](/features/) directory.

Maze Runner's CLI and the test fixtures are containerized so you'll need Docker (and Docker Compose) to run them.

__Note: only Bugsnag employees can run the end-to-end tests.__ We have dedicated test infrastructure and private BrowserStack credentials which can't be shared outside of the organization.

### Authenticating with the private container registry

You'll need to set the credentials for the aws profile in order to access the private docker registry:

```
aws configure --profile=opensource
```

Subsequently you'll need to run the following commmand to authenticate with the registry:

```
$(aws ecr get-login --profile=opensource --no-include-email)
```

__Your session will periodically expire__, so you'll need to run this command to re-authenticate when that happens.

Remote tests can be run against real devices provided by BrowserStack. In order to run these tests, you need to set the following environment variables:

- A BrowserStack App Automate Username: `MAZE_DEVICE_FARM_USERNAME`
- A BrowserStack App Automate Access Key: `MAZE_DEVICE_FARM_ACCESS_KEY`
- A path to a [BrowserStack local testing binary](https://www.browserstack.com/local-testing/app-automate): `MAZE_BS_LOCAL`

### Running an end-to-end test

1. Build the test fixtures `make test-fixtures` (separate fixtures are built with and without the NDK/ANR plugins)
1. Check the contents of `Gemfile` to select the version of `maze-runner` to use
1. To run a single feature:
    ```shell script
    make test-fixtures && \
    bundle exec maze-runner --app=build/fixture.apk                 \
                            --farm=bs                               \
                            --device=ANDROID_9_0                    \
                            features/smoke_tests/unhandled.feature
    ```
1. To run all features, omit the final argument, but be wary of how many tests you run locally as we have a limited number of parallel tests and local running subverts the controls we have in place.  For a full test run it is generally best to push your branch to Github and let CI run them.
1. Maze Runner also supports all options that Cucumber does.  Run `bundle exec maze-runner --help` for full details.
