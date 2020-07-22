Contributing
============

-   [Fork](https://help.github.com/articles/fork-a-repo) the [notifier on github](https://github.com/bugsnag/bugsnag-android)
-   Build and test your changes
-   Commit and push until you are happy with your contribution
-   [Make a pull request](https://help.github.com/articles/using-pull-requests)
-   Thanks!

## Reporting issues

Are you having trouble getting started? Please [contact us directly](mailto:support@bugsnag.com?subject=%5BGitHub%5D%20Android%20SDK%20-%20having%20trouble%20getting%20started%20with%20Bugsnag) for assistance with integrating Bugsnag into your application.
If you have spotted a problem with this module, feel free to open a [new issue](https://github.com/bugsnag/bugsnag-android/issues/new). Here are a few things to check before doing so:

* Are you using the latest version of `bugsnag-android`? If not, does updating to the latest version fix your issue?
* Has somebody else [already reported](https://github.com/bugsnag/bugsnag-android/issues?utf8=%E2%9C%93&q=is%3Aissue%20is%3Aopen) your issue? Feel free to add additional context to or check-in on an existing issue that matches your own.
* Is your issue caused by this module? Only things related to the `bugsnag-android` module should be reported here. For anything else, please [contact us directly](mailto:support@bugsnag.com) and we'd be happy to help you out.

### Fixing issues

If you've identified a fix to a new or existing issue, we welcome contributions!
Here are some helpful suggestions on contributing that help us merge your PR quickly and smoothly:

* [Fork](https://help.github.com/articles/fork-a-repo) the
  [library on GitHub](https://github.com/bugsnag/bugsnag-android)
* Build and test your changes using the example app and test suite
* Commit and push until you are happy with your contribution
* [Make a pull request](https://help.github.com/articles/using-pull-requests)
* Ensure the automated checks pass (and if it fails, please try to address the cause)

### Adding features

Unfortunately we’re unable to accept PRs that add features or refactor the library at this time.
However, we’re very eager and welcome to hearing feedback about the library so please contact us directly to discuss your idea, or open a
[feature request](https://github.com/bugsnag/bugsnag-android/issues/new?template=Feature_request.md) to help us improve the library.

Here’s a bit about our process designing and building the Bugsnag libraries:

* We have an internal roadmap to plan out the features we build, and sometimes we will already be planning your suggested feature!
* Our open source libraries span many languages and frameworks so we strive to ensure they are idiomatic on the given platform, but also consistent in terminology between platforms. That way the core concepts are familiar whether you adopt Bugsnag for one platform or many.
* Finally, one of our goals is to ensure our libraries work reliably, even in crashy, multi-threaded environments. Oftentimes, this requires an intensive engineering design and code review process that adheres to our style and linting guidelines.

### Updating dependencies

Most dependencies are controlled by the module-level gradle files, however
running the NDK C/C++ components also depends on
[`greatest`](https://github.com/silentbicycle/greatest) and [`parson`](https://github.com/kgabis/parson), managed by [clib](https://github.com/clibs/clib).
Both libraries are vendored into the repository and clib is not required unless
updating the dependencies.

To update a clib dependency, reinstall it. For example, using parson:

    clib install kgabis/parson -o src/test/cpp/deps --save


## Installing the Android SDK

Running `./gradlew` can automatically install both the Gradle build system
and the Android SDK.

If you already have the Android SDK installed, make sure to export the
`ANDROID_HOME` environment variable, for example:

```shell
export ANDROID_HOME=/usr/local/Cellar/android-sdk/23.0.2
```

If you don't already have the Android SDK installed, it will be automatically
installed to `~/.android-sdk`.

> Note: You'll need to make sure that the `adb`, `android` and `emulator` tools
> installed as part of the Android SDK are available in your `$PATH` before
> building.


## Building the Library

Be sure to clone the repository submodules:

```shell
git submodule update --init --recursive
```

You can build new `.aar` files as follows:

```shell
./gradlew assembleRelease
```

Files are generated into`<module>/build/outputs/aar`.

### Building with custom ABIs

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

## Testing

Full details of how to build and run tests can be found in [the testing guide](TESTING.md)

## Installing/testing against a local maven repository

Change `VERSION_NAME` in `gradle.properties` to a version higher than the currently
released bugsnag-android, then run:

```shell
./gradlew assembleRelease publishToMavenLocal
```

This installs bugsnag-android to a local maven repository. The example app should automatically use
this version when you next run the app.

# Releasing a New Version

Full details of how to release can be found in [the release guide](RELEASING.md)
