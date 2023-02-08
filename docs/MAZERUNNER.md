# Mazerunner tests

E2E tests are implemented with our notifier testing tool [Maze runner](https://github.com/bugsnag/maze-runner), which is a black-box test framework written in Ruby.

## Running remote end-to-end tests

End to end tests are written in cucumber-style `.feature` files, and need Ruby-backed "steps" in order to know what to run. The tests are located in the top level [`features`](/features/) directory.

__Note: only Bugsnag employees can run the end-to-end tests.__ We have dedicated test infrastructure and private BrowserStack credentials which can't be shared outside of the organization.

Remote tests can be run against real devices provided by BrowserStack. In order to run these tests, you need to set the following environment variables:

- A BrowserStack App Automate Username: `BROWSER_STACK_USERNAME`
- A BrowserStack App Automate Access Key: `BROWSER_STACK_ACCESS_KEY`
- A path to a [BrowserStack local testing binary](https://www.browserstack.com/local-testing/app-automate): `MAZE_BS_LOCAL`

### Setting up local end-to-end testing

1. Run `bundle install`
1. Create and export a GPG key (follow the instructions in [RELEASING](RELEASING.md))

### Running an end-to-end test

1. Check the contents of `Gemfile` to select the version of `maze-runner` to use
1. To run a single feature:
    ```shell script
    make fixture-r21 && \
    bundle exec maze-runner --app=build/fixture-r21.apk                 \
                            --farm=bs                               \
                            --device=ANDROID_9_0                    \
                            features/smoke_tests/04_unhandled.feature
    ```
1. To run all features, omit the final argument, but be wary of how many tests you run locally as we have a limited number of parallel tests and local running subverts the controls we have in place.  For a full test run it is generally best to push your branch to Github and let CI run them.
1. Maze Runner also supports all options that Cucumber does.  Run `bundle exec maze-runner --help` for full details.
