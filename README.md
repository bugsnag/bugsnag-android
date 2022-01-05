
# Bugsnag error monitoring & exception reporter for Android
[![Documentation](https://img.shields.io/badge/documentation-latest-blue.svg)](https://docs.bugsnag.com/platforms/android/)

Get comprehensive [Android crash reports](https://www.bugsnag.com/platforms/android/) to quickly debug errors.

Bugsnag's [Android crash reporting](https://www.bugsnag.com/platforms/android/)
library automatically detects crashes in your Android apps, collecting
diagnostic information and immediately notifying your development team, helping
you to understand and resolve issues as fast as possible.

## Features

* Automatically report unhandled exceptions and crashes
* Report [handled exceptions](https://docs.bugsnag.com/platforms/android/#reporting-handled-exceptions)
* [Log breadcrumbs](https://docs.bugsnag.com/platforms/android/#logging-breadcrumbs) which are attached to crash reports and add insight to users' actions
* [Attach user information](https://docs.bugsnag.com/platforms/android/#identifying-users) to determine how many people are affected by a crash


## Getting started

1. [Create a Bugsnag account](https://www.bugsnag.com)
1. Complete the instructions in the [integration guide](https://docs.bugsnag.com/platforms/android/) to report unhandled exceptions thrown from your app
1. Report handled exceptions using [`Bugsnag.notify`](https://docs.bugsnag.com/platforms/android/reporting-handled-exceptions/)
1. Customize your integration using the [configuration options](https://docs.bugsnag.com/platforms/android/configuration-options/)


## Support

* [Read the integration guide](https://docs.bugsnag.com/platforms/android/) or [configuration options documentation](https://docs.bugsnag.com/platforms/android/configuration-options/)
* [Search open and closed issues](https://github.com/bugsnag/bugsnag-android/issues?utf8=✓&q=is%3Aissue) for similar problems
* [Report a bug or request a feature](https://github.com/bugsnag/bugsnag-android/issues/new)


## Contributing

All contributors are welcome! For information on how to build, test
and release `bugsnag-android`, see our
[contributing guide](https://github.com/bugsnag/bugsnag-android/blob/master/CONTRIBUTING.md).

Bugsnag employees should start by reading [the docs](docs/README.md).

## License

The Bugsnag Android notifier is free software released under the MIT License.
See the [LICENSE](https://github.com/bugsnag/bugsnag-android/blob/master/LICENSE)
for details.
