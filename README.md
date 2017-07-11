# Bugsnag exception reporter for Android
[![Documentation](https://img.shields.io/badge/documentation-latest-blue.svg)](http://docs.bugsnag.com/platforms/android/)
[![Build status](https://travis-ci.org/bugsnag/bugsnag-android.svg?branch=master)](https://travis-ci.org/bugsnag/bugsnag-android)
<a href="http://www.methodscount.com/?lib=com.bugsnag%3Abugsnag-android%3A3.9.0"><img src="https://img.shields.io/badge/Methods and size-core: 661 | deps: 32 | 73 KB-e91e63.svg"/></a>


Bugsnag's [Android crash reporting](https://bugsnag.com/platforms/android)
library automatically detects crashes in your Android apps, collecting
diagnostic information and immediately notifying your development team, helping
you to understand and resolve issues as fast as possible.


## Features

* Automatically report unhandled exceptions and crashes
* Report [handled exceptions](http://docs.bugsnag.com/platforms/android/#reporting-handled-exceptions)
* [Log breadcrumbs](http://docs.bugsnag.com/platforms/android/#logging-breadcrumbs) which are attached to crash reports and add insight to users' actions
* [Attach user information](http://docs.bugsnag.com/platforms/android/#identifying-users) to determine how many people are affected by a crash


## Getting started

1. [Create a Bugsnag account](https://bugsnag.com)
1. Complete the instructions in the [integration guide](http://docs.bugsnag.com/platforms/android/) to report unhandled exceptions thrown from your app
1. Report handled exceptions using [`Bugsnag.notify`](http://docs.bugsnag.com/platforms/android/reporting-handled-exceptions/)
1. Customize your integration using the [configuration options](http://docs.bugsnag.com/platforms/android/configuration-options/)


## Support

* [Read the integration guide](http://docs.bugsnag.com/platforms/android/) or [configuration options documentation](http://docs.bugsnag.com/platforms/android/configuration-options/)
* [Search open and closed issues](https://github.com/bugsnag/bugsnag-android/issues?utf8=✓&q=is%3Aissue) for similar problems
* [Report a bug or request a feature](https://github.com/bugsnag/bugsnag-android/issues/new)


## Contributing

All contributors are welcome! For information on how to build, test
and release `bugsnag-android`, see our
[contributing guide](https://github.com/bugsnag/bugsnag-android/blob/master/CONTRIBUTING.md).


## License

The Bugsnag Android notifier is free software released under the MIT License.
See [LICENSE.txt](https://github.com/bugsnag/bugsnag-android/blob/master/LICENSE.txt)
for details.
