# Changelog

## 4.1.3 (2017-11-07)
- Compile annotations dependency as api rather than implementation
- Support handled state case for React Native

## 4.1.2 (2017-11-02)
- Allow setting device ID to null

## 4.1.1 (2017-10-12)
- Performance improvements to reduce execution time of `Bugsnag.init`

## 4.1.0 (2017-10-02)
- The SDK now automatically tracks whether an error is handled or unhandled.
- Fix for NPE in MetaData callback [Boris](https://github.com/borhub)
- Updated example app.
- Setting the maxSize of breadcrumbs now removes any surplus breadcrumbs.
- Crash reports on application startup are automatically sent synchronously on the next launch (configurable via `setLaunchCrashThresholdMs`).

## 4.0.0 (2017-08-15)

This is a major release which adds a number of new features to the library. The minimum SDK version supported by Bugsnag is now API 14.

### Breaking Changes

Identifying devices and users (unless overridden) is now captured and persisted
through a per-install generated UUID, replacing use of
`Settings.Secure.ANDROID_ID`. As a result, existing errors may see doubled user
counts if they continue to happen. If this is an issue, see the [upgrade
guide](UPGRADING.md#upgrade-from-3x-to-4x)
for more information and a workaround.

### Enhancements
- Support loading different API keys for different product flavors, through manifest placeholders

- Support custom HTTP Error Reporting clients, by allowing custom implementations of `ErrorReportApiClient`

- Add nullability annotations throughout application

- Any exceptions caused by StrictMode will automatically add the policy violation type to the Error Report's metadata

- All tests have been updated to use JUnit 4 rather than `AndroidTestCase`

- Javadoc for the public API is now hosted on Github Pages

- Breadcrumbs are automatically logged for each callback in the Activity Lifecycle, for all activities

- Breadcrumbs are automatically logged for most System Intents (e.g. `android.intent.action.CONFIGURATION_CHANGED`)

- Optimize how reports are sent relative to network connectivity to improve battery life

- Added documentation on how Breadcrumbs can be setup to track the Fragment Lifecycle

- Added Kotlin example app and documentation

- Disable logs by default on release builds

### Bug Fixes
- Handle `RejectionExecutionException` by writing unqueued Error reports to disk
[Damian Wieczorek](https://github.com/damianw)

- Handle IllegalStateException caused by `CharsetEncoder` on Android 6.0
[Ben Lee](https://github.com/Bencodes)

- Each implementation of `beforeNotify()` is now only called once, in the order in which it was added
[jermainedilao](https://github.com/jermainedilao)

- By default, the User ID is now a per-install UUID, whereas previously `Settings.Secure.ANDROID_ID` was used
[Martin Georgiev](https://github.com/georgiev-martin)

- Update Gradle dependencies
[Frieder Bluemle](https://github.com/friederbluemle)


## 3.9.0 (2017-05-08)

### Enhancements

* Improve performance by using bounded `ThreadPoolExecutor` for asynchronous
  `notify()` calls
  [Felipe Lima](https://github.com/felipecsl)
  [#145](https://github.com/bugsnag/bugsnag-android/pull/145)

* Detect systemless root
  [Matthias Urhahn](https://github.com/d4rken)
  [#142](https://github.com/bugsnag/bugsnag-android/pull/142)

## 3.8.0 (2017-01-27)

## Enhancements

* Add support for interfacing with native code

## 3.7.2 (2017-01-12)

* Cache unhandled exception reports prior to sending, send non-blocking
  [Delisa Mason](https://github.com/kattrali)
  [#139](https://github.com/bugsnag/bugsnag-android/pull/139)

## 3.7.1 (2016-12-21)

### Bug fixes

* Make `getContext` and `clearUser` static methods
  [Dave Perryman](https://github.com/Pezzah)
  [#132](https://github.com/bugsnag/bugsnag-android/pull/132)

* Ensure fatal crashes are sent as blocking requests
  [Simon Maynard](https://github.com/snmaynard)
  [#137](https://github.com/bugsnag/bugsnag-android/pull/137)

## 3.7.0 (2016-10-05)

### Enhancements

- Add support for sending reports using lambdas for customization
  [Delisa Mason](https://github.com/kattrali)
  [#123](https://github.com/bugsnag/bugsnag-android/pull/123)

## 3.6.0 (2016-09-09)

### Enhancements

- Support optionally persisting user information between sessions using the
  configuration option `persistUserBetweenSessions`
  [Dave Perryman](https://github.com/Pezzah)
  [#120](https://github.com/bugsnag/bugsnag-android/pull/120)

- Support initializing Bugsnag with a pre-configured Configuration instance
  [Dave Perryman](https://github.com/Pezzah)
  [#121](https://github.com/bugsnag/bugsnag-android/pull/121)

- Expose client context
  [nicous](https://github.com/nicous)
  [#112](https://github.com/bugsnag/bugsnag-android/pull/112)

- Add CPU/ABI information to device metadata
  [Dave Perryman](https://github.com/Pezzah)
  [Crossle Song](https://github.com/crossle)
  [#119](https://github.com/bugsnag/bugsnag-android/pull/119)

### Bug Fixes

- Fix potentially misdirected error report when changing the endpoint soon
  after initializing Bugsnag
  [Dave Perryman](https://github.com/Pezzah)
  [#121](https://github.com/bugsnag/bugsnag-android/pull/121)

- Fix missing static modifier on `disableExceptionHandler`
  [Niklas Klein](https://github.com/Taig)
  [#113](https://github.com/bugsnag/bugsnag-android/pull/113)

## 3.5.0 (2016-07-21)

### Enhancements

- Add access to new Breadcrumbs API
  [Delisa Mason](https://github.com/kattrali)
  [#111](https://github.com/bugsnag/bugsnag-android/pull/111)


## 3.4.0 (2016-03-09)

### Enhancements

- Limit the number of stored errors
  [Duncan Hewett](https://github.com/duncanhewett)
  [#97](https://github.com/bugsnag/bugsnag-android/pull/97)

### Bug Fixes

- Fix `ConcurrentModificationException` which could occur when saving
  breadcrumbs
  [Duncan Hewett](https://github.com/duncanhewett)
  [#98](https://github.com/bugsnag/bugsnag-android/pull/98)

- Localize all numbers in error metrics
  [Delisa Mason](https://github.com/kattrali)
  [#100](https://github.com/bugsnag/bugsnag-android/pull/100)

3.3.0 (2016-01-18)
-----

### Enhancements

- Change distribution method to be .aar only
  [Lars Grefer](https://github.com/larsgrefer)
  [#91](https://github.com/bugsnag/bugsnag-android/pull/91)

- Skip sending empty device data values
  [Matthias Urhahn](https://github.com/d4rken)
  [#96](https://github.com/bugsnag/bugsnag-android/pull/96)

- Remove the need for synthetic methods
  [Jake Wharton](https://github.com/JakeWharton)
  [#87](https://github.com/bugsnag/bugsnag-android/pull/87)

3.2.7 (2015-12-10)
-----

### Enhancements

- Add additional check to ensure the cache of uploaded errors are deleted
  [#80](https://github.com/bugsnag/bugsnag-android/issues/80)

### Bug Fixes

- Fix exception which occurs when `appContext.getResources()` is null
  [#78](https://github.com/bugsnag/bugsnag-android/issues/78)

- Fix bug preventing `maxBreadcrumbs` from being set
  [David Wu](https://github.com/wuman)
  [#70](https://github.com/bugsnag/bugsnag-android/pull/70)

3.2.6
-----
-   Add blocking API
-   Fix NPE issue
-   Concurrent adding to tabs
-   Thread Safe DateUtils#toISO8601

3.2.5
-----
-   Silence harmless proguard warning

3.2.4
-----
-   Support buildUUID to distinguish between multiple builds with the same appId and versionCode

3.2.3
-----
-   Support projectPackages when proguard is used.
-   Fix jailbroken % on Bugsnag dashboard.

3.2.2
-----
-   Prefer API keys passed to Client directly over those from AndroidManifest

3.2.1
-----
-   Fix NPE when unboxing in JsonStream (thanks @mattprecious)

3.2.0
-----
-   Allow setting Bugsnag API key in your AndroidManifest.xml

3.1.1
-----
-   Re-add `Error#getException` to allow access to exception in callbacks

3.1.0
-----
-   Add support for leaving developer-defined log messages called "breadcrumbs"
    to help understand what was happening in your application before each
    crash

3.0.0
-----
-   Removed dependency on `bugsnag-java`
-   Reduced memory usage, using lazy-loading and streaming
-   Easier "top activity" tracking
-   Device brand information is now collected (eg. "Samsung")
-   SSL enabled by default
-   Adding custom diagnostics (MetaData) is now easier to use
-   Fixed app name detection
-   Uses Android's new build system (gradle based)
-   Added unit tests, automatically run on travis.ci
-   Severity is now an Enum for type-safety

2.2.3
-----
-   Bump bugsnag-java dependency to fix prototype mismatch bug

2.2.2
-----
-   Add support for collecting thread-state information (enabled by default)

2.2.1
-----
-   Add support for beforeNotify callbacks
-   Allow disabling of automatic exception handler

2.2.0
-----
-   Add support for sending a custom app version with `setAppVersion`
-   Send both `versionName` and `versionCode` in the app tab

2.1.3
-----
-   Fix strictmode violation caused by hostname checking

2.1.2
-----
-   Prepare 'severity' feature for release

2.1.1
-----
-   Update bugsnag-java dependency, allows disabling of auto-notification

2.1.0
-----
-   Support severity
-   Better format of notification payload
-   Structure the data of notifications better

2.0.10
------
-   Fixed bug in `BugsnagFragmentActivity` calling the wrong callbacks.

2.0.9
-----
-   Added missing `setProjectPackages` and `setFilters` static methods
    to the `Bugsnag` class.

2.0.8
-----
-   Added additional `Activity` parent classes to help collect debug
    information, added support for custom Activities.

2.0.7
-----
-   Improved memory usage when sending exceptions that were previously
    saved to disk
-   `setNotifyReleaseStages` now defaults to `null`, to reduce confusion

2.0.6
-----
-   Fixed bug which caused notifications to be sent on the UI thread
    in some situations.
-   Fixed bug which meant `setIgnoreClasses` was not respected.

2.0.5
-----
-   Added support for `setIgnoreClasses` to set which exception classes
    should not be sent to Bugsnag.

2.0.4
-----
-   Attempt to automatically detect releaseStage from the debuggable flag
-   Android backward compatibility fixes, now works on Android 1.5+

2.0.3
-----
-   Fixed bug where session time wouldn't start counting until first exception

2.0.2
-----
-   Fixed missing apiKey in metrics, use java notifier's metrics sending

2.0.1
-----
-   Added `Bugsnag.addToTab` to replace `setExtraData` for sending meta-data
    with every exception
-   Reduced jar size

2.0.0
-----
-   Refactored to use the classes from `bugsnag-java`
-   Project is now available in Maven, and built using Maven
-   Added support for metrics tracking (MAU/DAU)
-   Added additional diagnostic information (network status, memory usage,
    gps status, time since boot, time since app load) to every notification
-   Fixed some issues which might have caused error sending to fail

1.0.2
-----
-   Ensure empty exception files aren't created

1.0.1
-----
-   Fix bug with incorrect "caused by" exception names and messages

1.0.0
-----
-   Initial release
