Upgrading Guide
===============

Upgrade from 5.X to 6.X
-----------------------

__This version contains several breaking changes__.

### Key points

- `redactedKeys` and `discardClasses` are now matched as a `Pattern` instead of `String`
- `ThreadType` has been removed in favour of `ErrorType` bringing bugsnag-android inline with other platform SDKs
- `Thread.id` is now a `String` instead of an `int` bringing bugsnag-android inline with other platform SDKs
- API Key validation has moved to `Bugsnag.start` (instead of when the `Configuration` is created), this means that `Bugsnag.start` will now fail with an exception if no API key is provided
- When no `BUILD_UUID` is specified one is automatically derived from your `.dex` files in order to match your bytecode to the appropriate `mapping.txt` file exactly. This behaviour can be opted-out of by setting `BUILD_UUID` to a blank (empty) string value:
  ```xml
  <meta-data android:name="com.bugsnag.android.BUILD_UUID" android:value="" />
  ```
- The deprecated `Configuration.launchCrashThresholdMs` (and equivalent manifest entry `LAUNCH_CRASH_THRESHOLD_MS`) have been removed in favour of `Configuration.launchDurationMillis` (`LAUNCH_DURATION_MILLIS`)

### redactedKeys & discardClasses

The properties / accessors for redacted keys and discard classes remain the same, but the type has been changed from `String` to `Pattern` to allow for more flexible matches (matching is done with `Pattern.matches`).

To retain the same behaviour as v5.X replace:

```kotlin
configuration.redactedKeys = setOf("password", "secret")
```

with

```kotlin
configuration.redactedKeys = setOf(
    Pattern.compile(".*password.*"),
    Pattern.compile(".*secret.*"),
)
```

### ThreadType removal

The `ThreadType` enum has been removed in favour of the existing `ErrorType` enum (this follows the same pattern as our other platform notifiers). Simply replace any references to `ThreadType` with `ErrorType`, the constant values remain the same:

```kotlin
Bugsnag.addOnError { event ->
    event.threads.first().type = ErrorType.UNKNOWN
    return@addOnError true
}
```

Upgrade from 4.X to 5.X
-----------------------

__This version contains many breaking changes__. It is part of an effort to unify our notifier libraries across platforms, making the user interface more consistent, and implementations better on multi-layered environments where multiple Bugsnag libraries need to work together (such as React Native).

### Key points

- `Bugsnag.init` has been renamed to `Bugsnag.start`.
- The configuration options available in the `AndroidManifest.xml` have been expanded and some have been renamed
- [ANR](https://developer.android.com/topic/performance/vitals/anr) and [NDK](https://developer.android.com/ndk/) crash detection have now been enabled by default. The types of error that Bugsnag detects can be configured using the [`enabledErrorTypes`](https://docs.bugsnag.com/platforms/android/configuration-options/#enablederrortypes) configuration option.
- The `BeforeNotify` and `BeforeSend` callbacks have been simplified to an `OnErrorCallback` that provides access to the data being sent to your dashboard.

More details of these changes can be found below and full documentation is available online at: [https://docs.bugsnag.com/platforms/android](https://docs.bugsnag.com/platforms/android).

### Bugsnag client

`Bugsnag.init` has been renamed to `Bugsnag.start`.

You should use the static `Bugsnag` interface rather than instantiating `Client` directly.

Many of the previous methods on `Bugsnag` should now be called on `Configuration` (or added to the `AndroidManifest.xml`) instead.

The full list of altered methods and their intended replacements can be found below:

| v4.x API                                                                               | v5.x API                                |
| -------------------------------------------------------------------------------------- | --------------------------------------- |
| `Client#Client(Context, String, boolean)`                                              | `Bugsnag#start(Context, Configuration)` |
| `Client#addToTab` or <br />`Bugsnag#addToTab`                                          | `Configuration#addMetadata` or <br />`Bugsnag#addMetadata(String, String, Object)` |
| `Client#beforeNotify` or <br />`Bugsnag#beforeNotify`                                  | `Configuration#addOnError` or <br />`Bugsnag#addOnError` |
| `Client#beforeRecordBreadcrumb` or <br />`Bugsnag#beforeRecordBreadcrumb`              | `Configuration#addOnBreadcrumb` or <br />`Bugsnag#addOnBreadcrumb` |
| `Client#clearBreadcrumbs` or <br />`Bugsnag#clearBreadcrumbs`                          | `Configuration#setMaxBreadcrumbs` or <br />`Bugsnag#addOnBreadcrumb` |
| `Client#clearTab` or <br />`Bugsnag#clearTab`                                          | `Configuration#clearMetadata` or <br />`Bugsnag#clearMetadata(String)` |
| `Client#clearUser` or <br />`Bugsnag#clearUser`                                        | `Configuration#setUser` or <br />`Bugsnag#setUser` |
| `Client#disableExceptionHandler` or <br />`Bugsnag#disableExceptionHandler`            | `Configuration#setAutoDetectErrors` or <br />`Configuration#setEnabledErrorTypes` |
| `Client#enableExceptionHandler` or <br />`Bugsnag#enableExceptionHandler`              | `Configuration#setAutoDetectErrors` or <br />`Configuration#setEnabledErrorTypes` |
| `Client#getConfig`                                                                     | Method removed as `Configuration` is now considered immutable after starting `Bugsnag`. |
| `Client#getMetaData` or <br />`Bugsnag#getMetaData`                                    | `Configuration#getMetadata` or <br />`Bugsnag#getMetadata` |
| `Client#init(Context)` or <br />`Bugsnag#init(Context)`                                | `Bugsnag#start(Context)` |
| `Client#init(Context, Configuration)` or <br />`Bugsnag#init(Context, Configuration)`  | `Bugsnag#start(Context, Configuration)` |
| `Client#init(Context, String)` or <br />`Bugsnag#init(Context, String)`                | `Bugsnag#start(Context, String)` |
| `Client#init(Context, String, boolean)` or <br />`Bugsnag#init(Context, String, boolean)` | `Bugsnag#start(Context, Configuration)` |
| `Client#leaveBreadcrumb(String, BreadcrumbType, Map<String,String>)` or <br />`Bugsnag#leaveBreadcrumb(String, BreadcrumbType, Map<String,String>)` | `Bugsnag#leaveBreadcrumb(String, BreadcrumbType, Map<String,Object>)` |
| `Client#notify(String, String, StackTraceElement[], Callback)` or <br />`Bugsnag#notify(String, String, StackTraceElement[], Callback)` | `Bugsnag#notify(Throwable, OnErrorCallback)` |
| `Client#notify(String, String, StackTraceElement[], Severity, MetaData)` or <br />`Bugsnag#notify(String, String, StackTraceElement[], Severity, MetaData)` | `Bugsnag#notify(Throwable, OnErrorCallback)` |
| `Client#notify(String, String, String, StackTraceElement[], Severity, MetaData)` or <br />`Bugsnag#notify(String, String, String, StackTraceElement[], Severity, MetaData)` | `Bugsnag#notify(Throwable, OnErrorCallback)` |
| `Client#notify(Throwable, Callback)` or <br />`Bugsnag#notify(Throwable, Callback)`    | `Bugsnag#notify(Throwable, OnErrorCallback)` |
| `Client#notify(Throwable, MetaData)` or <br />`Bugsnag#notify(Throwable, MetaData)`    | `Bugsnag#notify(Throwable, OnErrorCallback)` |
| `Client#notify(Throwable, Severity)` or <br />`Bugsnag#notify(Throwable, Severity)`    | `Bugsnag#notify(Throwable, OnErrorCallback)` |
| `Client#notify(Throwable, Severity, MetaData)` or <br />`Bugsnag#notify(Throwable, Severity, MetaData)` | `Bugsnag#notify(Throwable, OnErrorCallback)` |
| `Client#notifyBlocking(String, String, StackTraceElement[], Callback)`  | `Bugsnag#notify(Throwable, OnErrorCallback)` |
| `Client#notifyBlocking(String, String, StackTraceElement[], Severity, MetaData)`  | `Bugsnag#notify(Throwable, OnErrorCallback)` |
| `Client#notifyBlocking(String, String, String, StackTraceElement[], Severity, MetaData)`  | `Bugsnag#notify(Throwable, OnErrorCallback)` |
| `Client#notifyBlocking(Throwable)`                                                     | `Bugsnag#notify(Throwable, OnErrorCallback)` |
| `Client#notifyBlocking(Throwable, Callback)`                                           | `Bugsnag#notify(Throwable, OnErrorCallback)` |
| `Client#notifyBlocking(Throwable, MetaData)`                                           | `Bugsnag#notify(Throwable, OnErrorCallback)` |
| `Client#notifyBlocking(Throwable, Severity)`                                           | `Bugsnag#notify(Throwable, OnErrorCallback)` |
| `Client#notifyBlocking(Throwable, Severity, MetaData)`                                 | `Bugsnag#notify(Throwable, OnErrorCallback)` |
| `Client#setAppVersion` or <br />`Bugsnag#setAppVersion`                                | `Configuration#setAppVersion` |
| `Client#setAutoCaptureSessions` or <br />`Bugsnag#setAutoCaptureSessions`              | `Configuration#setAutoTrackSessions` |
| `Client#setBuildUUID` or <br />`Bugsnag#setBuildUUID`                                  | Method removed. Set as an AndroidManifest <meta-data> element instead using `com.bugsnag.android.BUILD_UUID` as the key |
| `Client#setEndpoint` or <br />`Bugsnag#setEndpoint`                                    | `Configuration#setEndpoints` |
| `Client#setErrorReportApiClient` or <br />`Bugsnag#setErrorReportApiClient`            | `Configuration#setDelivery` |
| `Client#setFilters` or <br />`Bugsnag#setFilters`                                      | `Configuration#setRedactedKeys` |
| `Client#setIgnoreClasses` or <br />`Bugsnag#setIgnoreClasses`                          | `Configuration#setDiscardClasses` |
| `Client#setLoggingEnabled` or <br />`Bugsnag#setLoggingEnabled`                        | `Configuration#setLogger` |
| `Client#setMaxBreadcrumbs` or <br />`Bugsnag#setMaxBreadcrumbs`                        | `Configuration#setMaxBreadcrumbs` |
| `Client#setMetaData` or <br />`Bugsnag#setMetaData`                                    | `Configuration#addMetadata` or <br /> `Bugsnag#addMetadata(String, String, Object)` |
| `Client#setNotifyReleaseStages` or <br />`Bugsnag#setNotifyReleaseStages`              | `Configuration#setEnabledReleaseStages` |
| `Client#setProjectPackages` or <br />`Bugsnag#setProjectPackages`                      | `Configuration#setProjectPackages` |
| `Client#setReleaseStage` or <br />`Bugsnag#setReleaseStage`                            | `Configuration#setReleaseStage` |
| `Client#setSendThreads` or <br />`Bugsnag#setSendThreads`                              | `Configuration#setSendThreads` |
| `Client#setSessionTrackingApiClient` or <br />`Bugsnag#setSessionTrackingApiClient`    | `Configuration#setDelivery` |
| `Client#setUserEmail` or <br />`Bugsnag#setUserEmail`                                  | `Configuration#setUser` or <br /> `Bugsnag#setUser` |
| `Client#setUserId` or <br />`Bugsnag#setUserId`                                        | `Configuration#setUser` or <br /> `Bugsnag#setUser` |
| `Client#setUserName` or <br />`Bugsnag#setUserName`                                    | `Configuration#setUser` or <br /> `Bugsnag#setUser` |
| `Client#stopSession` or <br />`Bugsnag#stopSession`                                    | `Bugsnag#pauseSession` |

See the [online documentation](https://docs.bugsnag.com/platforms/android) for more information.

### Configuration

Many more configuration options can be set via your `AndroidManifest.xml`, for example:

```xml
<meta-data
  android:name="com.bugsnag.android.MAX_BREADCRUMBS"
  android:value="35" />
```

Bugsnag can then be initialized by simply calling `Bugsnag.start`. 

You can also create a `Configuration` object yourself using `Configuration.load` to read the app manifest and then provide further configuration in code:

```kotlin
val config = Configuration.load(this)
config.maxBreadcrumbs = 35
Bugsnag.start(this, config)
```

Altering values on `Configuration` _after_ calling `Bugsnag.start` now has no effect. You should specify any non-default behavior up-front before Bugsnag is started.

Several methods on `Configuration` have been renamed for greater API consistency. A full list is shown below:

| v4.x API                                           | v5.x API                                           |
| -------------------------------------------------- | -------------------------------------------------- |
| `Configuration#beforeSend`                         | `Configuration#addOnError` |
| `Configuration#getErrorApiHeaders`                 | `Configuration#setDelivery` |
| `Configuration#getSessionApiHeaders`               | `Configuration#setDelivery` |
| `Configuration#setAutomaticallyCollectBreadcrumbs` | `Configuration#setEnabledBreadcrumbTypes` |
| `Configuration#setAnrThresholdMs`                  | Method no longer required. |
| `Configuration#setAutoCaptureSessions`             | `Configuration#setAutoTrackSessions` |
| `Configuration#setBuildUUID`                       | Method removed. Set as an AndroidManifest <meta-data> element instead using `com.bugsnag.android.BUILD_UUID` as the key |
| `Configuration#setDetectAnrs`                      | `Configuration#setAutoDetectErrors` or <br />`Configuration#setEnabledErrorTypes` |
| `Configuration#setDetectNdkCrashes`                | `Configuration#setAutoDetectErrors` or <br />`Configuration#setEnabledErrorTypes` |
| `Configuration#setEnableExceptionHandler`          | `Configuration#setAutoDetectErrors` or <br />`Configuration#setEnabledErrorTypes` |
| `Configuration#setEndpoint`                        | `Configuration#setEndpoints` |
| `Configuration#setFilters`                         | `Configuration#setRedactedKeys` |
| `Configuration#setMetaData`                        | `Configuration#addMetadata(String, String, Object)` |
| `Configuration#setNotifierType`                    | Method removed as end-users should not alter this value. |
| `Configuration#setNotifyReleaseStages`             | `Configuration#setEnabledReleaseStages` |
| `Configuration#setPersistUserBetweenSessions`      | `Configuration#setPersistUser` |
| `Configuration#setSessionEndpoint`                 | `Configuration#setEndpoints` |

See the [online documentation](https://docs.bugsnag.com/platforms/android/configuration-options/) for more information.

### BeforeNotify/BeforeSend/Callback

These three callbacks have been superseded and replaced by `OnError`, a single callback which runs immediately after an `Event` has been captured. This can run globally on all events, or on an individual event.

```kotlin
val config = Configuration.load(this)
config.addOnError { event ->
    event.context = "Some Custom context"
    true
})
Bugsnag.start(this, config)

// run on handled error
Bugsnag.notify(myThrowable) { event ->
    event.context = "My Unique context"
    true
}
```

If you use the NDK, implement the native `on_error` callback which will be run for fatal C/C++ errors only. See [below](#v5-ndk-changes)

See the [online documentation](https://docs.bugsnag.com/platforms/android/customizing-error-reports/) for more information.

### Error -> Event

`Error` has been replaced by `Event`, which represents the payload that will be sent to Bugsnag's API. A large number of new accessors have been added to the `Event` class to allow for greater customization of error reports in callbacks.

Several existing methods have been renamed, a full list of which is shown below:

| v4.x API                     | v5.x API |
| ---------------------------- | ---------------------------- |
| `Error#addToTab`             | `Event#addMetadata(String, String, Object)` |
| `Error#clearTab`             | `Event#clearMetadata(String)` |
| `Error#getDeviceData`        | `Event#getDevice` |
| `Error#getException`         | `Event#getOriginalError` |
| `Error#getHandledState`      | `Event#isUnhandled` |
| `Error#getMetaData`          | `Event#getMetadata(String)` |
| `Error#setDeviceId`          | `Event#getDevice` |
| `Error#setExceptionMessage`  | `Event#setErrors` |
| `Error#setExceptionName`     | `Event#setErrors` |
| `Error#setMetaData`          | `Event#addMetadata(String, String, Object)` |
| `Error#setUserEmail`         | `Event#setUser(String, String, String)` |
| `Error#setUserId`            | `Event#setUser(String, String, String)` |
| `Error#setUserName`          | `Event#setUser(String, String, String)` |

See the [online documentation](https://docs.bugsnag.com/platforms/android/customizing-error-reports/) for more information.

### Breadcrumbs

Breadcrumbs now contain a message rather than a name and the callback has been renamed for consistency:

| v4.x API                              | v5.x API |
| ------------------------------------- | ------------------------------------- |
| `Breadcrumb#getName`                  | `Breadcrumb#getMessage` |
| `BeforeRecordBreadcrumb#shouldRecord` | `OnBreadcrumbCallback#onBreadcrumb` |

See the [online documentation](https://docs.bugsnag.com/platforms/android/customizing-breadcrumbs/) for more information.


### Session payload & callbacks

`SessionTrackingPayload` is now called `SessionPayload`. It is now possible to redact the `app` and `device` information captured on sessions via an `OnSessionCallback`.

See the [online documentation](https://docs.bugsnag.complatforms/android/capturing-sessions/) for more information.

### Delivery

The signature for providing a custom delivery mechanism has changed:

| v4.x API  | v5.x API |
| --------------------------------------------------------- | --------------------------------------------------------- |
| `Delivery#deliver(Report, Configuration)`                 | `Delivery#deliver(SessionPayload, DeliveryParams)` |
| `Delivery#deliver(SessionTrackingPayload, Configuration)` | `Delivery#deliver(Report, DeliveryParams)` |

See the [online documentation](https://docs.bugsnag.com/platforms/android/configuration-options/) for more information.

### Removed from public API

| v4.x API  | v5.x API |
| --------------------------------------------------------- | --------------------------------------------------------- |
| `MetaData`                 | No longer publicly accessible, and should be added via the `Bugsnag` interface or via an `OnError` callback. |
| `Report`                   | This class is no longer publicly accessible as end-users should not need to set its values. The `Event` class should now be used to alter the details of an error report.  |
| `ErrorReportApiClient`     | Previously deprecated, use `Delivery` interface instead. |
| `SessionTrackingApiClient` | Previously deprecated, use `Delivery` interface instead. |
| `BadResponseException`     | Previously deprecated, use `Delivery` interface instead. |
| `DeliveryFailureException` | Previously deprecated, use `Delivery` interface instead. |
| `NetworkException`         | Previously deprecated, use `Delivery` interface instead. |
| `Notifier`                 | This class is no longer publicly accessible as end-users should not need to set its values. |
| `EventReceiver`            | This class is no longer publicly accessible as end-users should not need to set its values. |
| `BugsnagException`         | This class is no longer required - you should use your own `Throwable` instead. |

<div id="v5-ndk-changes"></div>

### NDK changes

#### Breaking changes 

- The `report.h` header has been renamed to `event.h`
- `bugsnag_init()` has been renamed to `bugsnag_start()`
- `bsg_severity_t` has been renamed to `bugsnag_severity`
- `bsg_breadcrumb_t` has been renamed to `bugsnag_breadcrumb_type`

#### New functionality

Native events can now be customized by adding an `on_error` callback. This allows you to amend the
 data that will be sent to your Bugsnag dashboard for fatal NDK crashes.

```c
bool custom_on_error_callback(void *event) {
    bugsnag_event_set_severity(event, BSG_SEVERITY_WARN);
    // ...
}
bugsnag_add_on_error(&custom_on_error_callback);
```

Most of the fields in the `Event` class in the JVM are available in this callback using a set of functions (see [event.h](https://github.com/bugsnag/bugsnag-android/blob/master/bugsnag-plugin-android-ndk/src/main/assets/include/event.h))
 that take the event pointer argument and either return data from the event or allow you to set it.
Full examples are given in the [Event class documentation](https://docs.bugsnag.com/platforms/android/customizing-error-reports/#the-event-class).

Care must be taken to ensure that the callback function provided is async-signal safe. The code will
be executed in an asynchronous signal handler and so must not call any functions that are not Async-Signal-Safe.
Further information can be found [here](https://wiki.sei.cmu.edu/confluence/display/c/SIG30-C.+Call+only+asynchronous-safe+functions+within+signal+handlers).

### Metadata changes

- Removed `packageName` from the `app` metadata tab, as the field is duplicated by `app.id`
- Removed `versionName` from the `app` metadata tab, as the field is duplicated by `app.version` and this has been known to cause confusion amongst users in the past

Upgrade bugsnag-android-ndk from 1.x to 4.x
-----------------------

This is a backwards-compatible release. Update the version number to 4.+ to
upgrade:

```groovy
dependencies {
  implementation 'com.bugsnag:bugsnag-android-ndk:4.+'
}
```

Upgrade from 3.X to 4.X
-----------------------
- Increase the `minSdkVersion` to 14 or higher in your app's build.gradle file.
- The default method for identifying devices and users is now captured and
  persisted through a per-install generated UUID, replacing use of
  `Settings.Secure.ANDROID_ID`. As a result, existing errors on your Bugsnag
  dashboard may see doubled user counts if they continue to occur after
  upgrading the library. If this is an issue, a workaround would be to assign
  the user identifier using the previous value.

  ```java
  String androidId = Settings.Secure.getString(cr, Settings.Secure.ANDROID_ID);
  Bugsnag.setUserId(androidId)
  ```

  For more information and discussion see
  [#83](https://github.com/bugsnag/bugsnag-android/issues/83)


Upgrade from 2.0 to 3.0
-----------------------
-   Change any `Bugsnag.register` calls to `Bugsnag.init`:

    ```java
    // Old
    Bugsnag.register(Context, "api-key");

    // New
    Bugsnag.init(Context, "api-key");
    ```

-   Severity is now an `Enum`, so please update any references to severity in your app:

    ```java
    // Old
    Bugsnag.notify(new RuntimeException("oops"), "error");

    // New
    import com.bugsnag.android.Severity;
    Bugsnag.notify(new RuntimeException("oops"), Severity.ERROR);
    ```

-   "Top Activity" tracking is now automatically handled when you add the
    `GET_TASKS` permission to your `AndroidManifest.xml`, so you should no
    longer have your app's `Activity`s inherit from `BugsnagActivity`:

    ```java
    // Old
    class MyActivity extends BugsnagActivity {}

    // New
    class MyActivity extends Activity {}
    ```
