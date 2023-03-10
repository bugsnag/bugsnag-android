<a href="https://www.bugsnag.com/platforms/android">
  <h1 align="center">
    <picture>
      <source media="(prefers-color-scheme: dark)" srcset="https://assets.smartbear.com/m/3dab7e6cf880aa2b/original/BugSnag-Repository-Header-Dark.png">
      <img alt="BugSnag" src="https://assets.smartbear.com/m/3945e02cdc983893/original/BugSnag-Repository-Header-Light.png">
    </picture>
  </h1>
</a>

[![Documentation](https://img.shields.io/badge/documentation-latest-blue.svg)](https://docs.bugsnag.com/platforms/android/)
[![Build status](https://badge.buildkite.com/ff6aa35c92e06a739cb095b58762dffab8011c7f05a1ce86e1.svg?branch=master)](https://buildkite.com/bugsnag/bugsnag-android)

BugSnag's [Android crash reporting](https://www.bugsnag.com/platforms/android/) library automatically detects crashes in your Android apps, collecting diagnostic information and immediately notifying your development team, helping you to understand and resolve issues as fast as possible.

# Features

* Automatically report unhandled exceptions and crashes
* Report [handled exceptions](https://docs.bugsnag.com/platforms/android/#reporting-handled-exceptions)
* [Log breadcrumbs](https://docs.bugsnag.com/platforms/android/#logging-breadcrumbs) which are attached to crash reports and add insight to users' actions
* [Attach user information](https://docs.bugsnag.com/platforms/android/#identifying-users) to determine how many people are affected by a crash


# Getting started

1. [Create a BugSnag account](https://www.bugsnag.com)
1. Complete the instructions in the [integration guide](https://docs.bugsnag.com/platforms/android/) to report unhandled exceptions thrown from your app
1. Report handled exceptions using [`Bugsnag.notify`](https://docs.bugsnag.com/platforms/android/reporting-handled-exceptions/)
1. Customize your integration using the [configuration options](https://docs.bugsnag.com/platforms/android/configuration-options/)


# Support

* [Read the integration guide](https://docs.bugsnag.com/platforms/android/) or [configuration options documentation](https://docs.bugsnag.com/platforms/android/configuration-options/)
* [Search open and closed issues](https://github.com/bugsnag/bugsnag-android/issues?utf8=✓&q=is%3Aissue) for similar problems
* [Report a bug or request a feature](https://github.com/bugsnag/bugsnag-android/issues/new)


# Contributing

All contributors are welcome! For information on how to build, test and release `bugsnag-android`, see our [contributing guide](https://github.com/bugsnag/bugsnag-android/blob/master/CONTRIBUTING.md). BugSnag employees should start by reading [the docs](docs/README.md).

# License

The BugSnag Android notifier is free software released under the MIT License. See the [LICENSE](https://github.com/bugsnag/bugsnag-android/blob/master/LICENSE) for details.
