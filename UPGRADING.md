Upgrading Guide
===============

Upgrade from 4.X to 5.X
-----------------------

__This version contains many breaking changes__. It is part of an effort to unify our notifier libraries across platforms, making the user interface more consistent, and implementations better on multi-layered environments where multiple Bugsnag libraries need to work together (such as React Native).

# Interfaces

## Bugsnag

`Bugsnag.init()` has been renamed to `Bugsnag.start()`.

Many of the previous methods on `Bugsnag` should now be called on `Configuration` instead.
For example, `maxBreadcrumbs` can now only be set on `Configuration` before `Bugsnag` is started:

```kotlin
val config = Configuration("my-api-key")
config.maxBreadcrumbs = 35
Bugsnag.start(this, config)
```

The full list of altered methods and their intended replacements can be found below:

| v4.x API  | v5.x API |
| ------------- | ------------- |
| `Bugsnag#addToTab`  | `Bugsnag#addMetadata(String, String, Object)` and `Configuration#addMetadata` |
| `Bugsnag#beforeNotify` | `Bugsnag#addOnError` and `Configuration#addOnError` |
| `Bugsnag#beforeRecordBreadcrumb` | `Bugsnag#addOnBreadcrumb` and `Configuration#addOnBreadcrumb` |
| `Bugsnag#clearBreadcrumbs` | `Bugsnag#addOnBreadcrumb/Configuration#setMaxBreadcrumbs` |
| `Bugsnag#clearTab` | `Bugsnag#clearMetadata(String)` and `Configuration#clearMetadata` |
| `Bugsnag#clearUser` | `Bugsnag#setUser` and `Configuration#setUser` |
| `Bugsnag#disableExceptionHandler` | `Configuration#setEnabledErrorTypes` and `Configuration#setAutoDetectErrors` |
| `Bugsnag#enableExceptionHandler` | `Configuration#setEnabledErrorTypes` and `Configuration#setAutoDetectErrors` |
| `Bugsnag#getMetaData`  | `Bugsnag#getMetadata` and `Configuration#getMetadata` |
| `Bugsnag#init(Context)` | `Bugsnag#start(Context)` |
| `Bugsnag#init(Context, Configuration)` | `Bugsnag#start(Context, Configuration)` |
| `Bugsnag#init(Context, String)` | `Bugsnag#start(Context, String)` |
| `Bugsnag#init(Context, String, boolean)` | `Bugsnag#start(Context, Configuration)` |
| `Bugsnag#leaveBreadcrumb(String, BreadcrumbType, Map<String,String>)` | `Bugsnag#leaveBreadcrumb(String, BreadcrumbType, Map<String,Object>)` |
| `Bugsnag#notify(String, String, StackTraceElement[], Callback)` | `Bugsnag#notify(Throwable, OnErrorCallback)` |
| `Bugsnag#notify(String, String, StackTraceElement[], Severity, MetaData)` | `Bugsnag#notify(Throwable, OnErrorCallback)` |
| `Bugsnag#notify(String, String, String, StackTraceElement[], Severity, MetaData)` | `Bugsnag#notify(Throwable, OnErrorCallback)` |
| `Bugsnag#notify(Throwable, Callback)` | `Bugsnag#notify(Throwable, OnErrorCallback)` |
| `Bugsnag#notify(Throwable, MetaData)` | `Bugsnag#notify(Throwable, OnErrorCallback)` |
| `Bugsnag#notify(Throwable, Severity)` | `Bugsnag#notify(Throwable, OnErrorCallback)` |
| `Bugsnag#notify(Throwable, Severity, MetaData)` | `Bugsnag#notify(Throwable, OnErrorCallback)` |
| `Bugsnag#stopSession` | `Bugsnag#pauseSession` |
| `Bugsnag#setAppVersion` | `Configuration#setAppVersion` |
| `Bugsnag#setAutoCaptureSessions` | `Configuration#setAutoTrackSessions` |
| `Bugsnag#setBuildUUID` | `Configuration#setBuildUuid` |
| `Bugsnag#setEndpoint` | `Configuration#setEndpoints` |
| `Bugsnag#setErrorReportApiClient` | `Configuration#setDelivery` |
| `Bugsnag#setFilters` | `Configuration#setRedactedKeys` |
| `Bugsnag#setIgnoreClasses` | `Configuration#setIgnoreClasses` |
| `Bugsnag#setLoggingEnabled` | `Configuration#setLogger` |
| `Bugsnag#setMaxBreadcrumbs` | `Configuration#setMaxBreadcrumbs` |
| `Bugsnag#setMetaData` | `Bugsnag#addMetadata(String, String, Object)` and `Configuration#addMetadata` |
| `Bugsnag#setNotifyReleaseStages` | `Configuration#setEnabledReleaseStages` |
| `Bugsnag#setProjectPackages` | `Configuration#setProjectPackages` |
| `Bugsnag#setReleaseStage` | `Configuration#setReleaseStage` |
| `Bugsnag#setSendThreads` | `Configuration#setSendThreads` |
| `Bugsnag#setSessionTrackingApiClient` | `Configuration#setDelivery` |
| `Bugsnag#setUserEmail` | `Bugsnag#setUser` and `Configuration#setUser` |
| `Bugsnag#setUserId` | `Bugsnag#setUser` and `Configuration#setUser` |
| `Bugsnag#setUserName` | `Bugsnag#setUser` and `Configuration#setUser` |

See the [full documentation](https://docs.bugsnag.com/platforms/android) for more information.

## Client

You should should use the static `Bugsnag` interface rather than instantiating `Client` directly. 

Many of the previous methods on `Client` should now be called on `Configuration` instead.
For example, `maxBreadcrumbs` can now only be set on `Configuration` before `Bugsnag` is started:

```kotlin
val config = Configuration("my-api-key")
config.maxBreadcrumbs = 35
Client(this, config)
```

The full list of altered methods and their intended replacements can be found below:

| v4.x API  | v5.x API |
| ------------- | ------------- |
| `Client#Client(Context, String, boolean)`  | `Bugsnag#start(Context, Configuration)` |
| `Client#addToTab`  | `Bugsnag#addMetadata(String, String, Object)` and `Configuration#addMetadata` |
| `Client#beforeNotify`  | `Bugsnag#addOnError` and `Configuration#addOnError` |
| `Client#beforeRecordBreadcrumb`  | `Bugsnag#addOnBreadcrumb` and `Configuration#addOnBreadcrumb` |
| `Client#clearBreadcrumbs`  | `Bugsnag#addOnBreadcrumb/Configuration#setMaxBreadcrumbs` |
| `Client#clearTab`  | `Bugsnag#clearMetadata(String)` and `Configuration#clearMetadata` |
| `Client#clearUser`  | `Bugsnag#setUser` and `Configuration#setUser` |
| `Client#disableExceptionHandler`  | `Configuration#setEnabledErrorTypes` and `Configuration#setAutoDetectErrors` |
| `Client#enableExceptionHandler`  | `Configuration#setEnabledErrorTypes` and `Configuration#setAutoDetectErrors` |
| `Client#getConfig`  | Method removed as `Configuration` is now considered immutable after starting `Bugsnag`. |
| `Client#getMetaData`  | `Bugsnag#getMetaData` and `Configuration#getMetaData` |
| `Client#leaveBreadcrumb(String, BreadcrumbType, Map<String,String>)`  | `Bugsnag#leaveBreadcrumb(String, BreadcrumbType, Map<String,Object>)` |
| `Client#notify(String, String, StackTraceElement[], Callback)`  | `Bugsnag#notify(Throwable, OnErrorCallback)` |
| `Client#notify(String, String, StackTraceElement[], Severity, MetaData)`  | `Bugsnag#notify(Throwable, OnErrorCallback)` |
| `Client#notify(String, String, String, StackTraceElement[], Severity, MetaData)`  | `Bugsnag#notify(Throwable, OnErrorCallback)` |
| `Client#notify(Throwable)`  | `Bugsnag#notify(Throwable)` |
| `Client#notify(Throwable, Callback)`  | `Bugsnag#notify(Throwable, OnErrorCallback)` |
| `Client#notify(Throwable, MetaData)`  | `Bugsnag#notify(Throwable, OnErrorCallback)` |
| `Client#notify(Throwable, Severity)`  | `Bugsnag#notify(Throwable, OnErrorCallback)` |
| `Client#notify(Throwable, Severity, MetaData)`  | `Bugsnag#notify(Throwable, OnErrorCallback)` |
| `Client#notifyBlocking(String, String, StackTraceElement[], Callback)`  | `Bugsnag#notify(Throwable, OnErrorCallback)` |
| `Client#notifyBlocking(String, String, StackTraceElement[], Severity, MetaData)`  | `Bugsnag#notify(Throwable, OnErrorCallback)` |
| `Client#notifyBlocking(String, String, String, StackTraceElement[], Severity, MetaData)`  | `Bugsnag#notify(Throwable, OnErrorCallback)` |
| `Client#notifyBlocking(Throwable)`  | `Bugsnag#notify(Throwable, OnErrorCallback)` |
| `Client#notifyBlocking(Throwable, Callback)`  | `Bugsnag#notify(Throwable, OnErrorCallback)` |
| `Client#notifyBlocking(Throwable, MetaData)`  | `Bugsnag#notify(Throwable, OnErrorCallback)` |
| `Client#notifyBlocking(Throwable, Severity)`  | `Bugsnag#notify(Throwable, OnErrorCallback)` |
| `Client#notifyBlocking(Throwable, Severity, MetaData)`  | `Bugsnag#notify(Throwable, OnErrorCallback)` |
| `Client#setAppVersion`  | `Configuration#setAppVersion` |
| `Client#setAutoCaptureSessions`  | `Configuration#setAutoTrackSessions` |
| `Client#setBuildUUID`  | `Configuration#setBuildUuid` |
| `Client#setEndpoint`  | `Configuration#setEndpoints` |
| `Client#setFilters`  | `Configuration#setRedactedKeys` |
| `Client#setIgnoreClasses`  | `Configuration#setIgnoreClasses` |
| `Client#setLoggingEnabled`  | `Configuration#setLogger` |
| `Client#setMaxBreadcrumbs`  | `Configuration#setMaxBreadcrumbs` |
| `Client#setMetaData`  | `Configuration#addMetadata` |
| `Client#setNotifyReleaseStages`  | `Configuration#setEnabledReleaseStages` |
| `Client#setProjectPackages`  | `Configuration#setProjectPackages` |
| `Client#setReleaseStage`  | `Configuration#setReleaseStage` |
| `Client#setSendThreads`  | `Configuration#setSendThreads` |
| `Client#setUserEmail`  | `Bugsnag#setUser` and `Configuration#setUser` |
| `Client#setUserId`  | `Bugsnag#setUser` and `Configuration#setUser` |
| `Client#setUserName`  | `Bugsnag#setUser` and `Configuration#setUser` |
| `Client#stopSession`  | `Bugsnag#pauseSession` and `Configuration#setUser` |

See the [full documentation](https://docs.bugsnag.com/platforms/android) for more information.

## Configuration

Altering values on `Configuration` _after_ calling `Bugsnag.start()` now has no effect. You should
specify any non-default behaviour up-front before Bugsnag is initialised.

```kotlin
val config = Configuration("my-api-key")
config.maxBreadcrumbs = 35
Bugsnag.start(this, config)
```

It is also possible to supply primitive configuration values via your `AndroidManifest.xml`.

```xml
<meta-data
  android:name="com.bugsnag.android.API_KEY"
  android:value="your-api-key" />
<meta-data
  android:name="com.bugsnag.android.MAX_BREADCRUMBS"
  android:value="35" />
```

```kotlin
Bugsnag.start(this)
```



Several methods on `Configuration` have been renamed for greater API consistency. A full list is shown below:

| v4.x API  | v5.x API |
| ------------- | ------------- |
| `Configuration#beforeSend`  | `Configuration#addOnError` |
| `Configuration#getErrorApiHeaders`  | `Configuration#setDelivery` |
| `Configuration#getSessionApiHeaders`  | `Configuration#setDelivery` |
| `Configuration#setAutomaticallyCollectBreadcrumbs`  | `Configuration#setEnabledBreadcrumbTypes` |
| `Configuration#setAnrThresholdMs`  | `Configuration#setEnabledErrorTypes` and `Configuration#setAutoDetectErrors` |
| `Configuration#setAutoCaptureSessions`  | `Configuration#setAutoTrackSessions` |
| `Configuration#setBuildUUID`  | `Configuration#setBuildUuid` |
| `Configuration#setDetectAnrs`  | `Configuration#setEnabledErrorTypes` and `Configuration#setAutoDetectErrors` |
| `Configuration#setDetectNdkCrashes`  | `Configuration#setEnabledErrorTypes` and `Configuration#setAutoDetectErrors` |
| `Configuration#setEnableExceptionHandler`  | `Configuration#setEnabledErrorTypes` and `Configuration#setAutoDetectErrors` |
| `Configuration#setEndpoint`  | `Configuration#setEndpoints` |
| `Configuration#setFilters`  | `Configuration#setRedactedKeys` |
| `Configuration#setMetaData`  | `Configuration#addMetadata(String, String, Object)` |
| `Configuration#setNotifierType`  | Method removed as end-users should not alter this value. |
| `Configuration#setNotifyReleaseStages`  | `Configuration#setEnabledReleaseStages` |
| `Configuration#setPersistUserBetweenSessions`  | `Configuration#setPersistUser` |
| `Configuration#setSessionEndpoint`  | `Configuration#setEndpoints` |

See the [full documentation](https://docs.bugsnag.com/platforms/android) for more information.

## Error

`Error` has been replaced by `Event`, which represents a JSON payload that will be sent to Bugsnag's API.
A large number of new accessors have been added to the `Event` class to allow for greater customization of error reports in callbacks.

Several existing methods have been renamed, a full list of which is shown below:

| v4.x API  | v5.x API |
| ------------- | ------------- |
| `Error#addToTab`  | `Event#addMetadata(String, String, Object)` |
| `Error#clearTab`  | `Event#clearMetadata(String)` |
| `Error#getDeviceData`  | `Event#getDevice` |
| `Error#getException`  | `Event#getOriginalError` |
| `Error#getHandledState`  | `Event#isUnhandled` |
| `Error#getMetaData`  | `Event#getMetadata(String)` |
| `Error#setDeviceId`  | `Event#getDevice` |
| `Error#setExceptionMessage`  | `Event#setErrors` |
| `Error#setExceptionName`  | `Event#setErrors` |
| `Error#setMetaData`  | `Event#addMetadata(String, String, Object)` |
| `Error#setUserEmail`  | `Event#setUser(String, String, String)` |
| `Error#setUserId`  | `Event#setUser(String, String, String)` |
| `Error#setUserName`  | `Event#setUser(String, String, String)` |

See the [full documentation](https://docs.bugsnag.com/platforms/android) for more information.

## BeforeNotify/BeforeSend/Callback

These three callbacks have been superseded and replaced by `OnError`, a single callback which runs immediately after an error has occurred. This can run globally on all errors, or on an individual error.

```kotlin
val config = Configuration("my-api-key")
config.maxBreadcrumbs = 35
config.addOnError { event -> // run on all errors
    event.context = "Some Custom context"
    return true
}
Bugsnag.start(this, config)

// run on single error only
Bugsnag.notify(myThrowable) { event ->
    event.context = "My Unique context"
    return true
}
```

If you use the NDK, you will need to implement your own native `on_error` callback _in addition_ to any JVM callbacks. This native `on_error` callback will run for fatal C/C++ errors only.

See the [full documentation](https://docs.bugsnag.com/platforms/android) for more information.

## Breadcrumb

Breadcrumbs now contain a message rather than a name:

| v4.x API  | v5.x API |
| ------------- | ------------- |
| `Breadcrumb#getName`  | `Breadcrumb#getMessage` |

## SessionTrackingPayload
`SessionTrackingPayload` is now called `SessionPayload`.

It is now possible to redact the `app` and `device` information captured on sessions via an `OnSessionCallback`.
See the [full documentation](https://docs.bugsnag.com/platforms/android) for more information.

## BeforeRecordBreadcrumb
`BeforeRecordBreadcrumb` is now called `OnBreadcrumbCallback`.

| v4.x API  | v5.x API |
| ------------- | ------------- |
| `BeforeRecordBreadcrumb#shouldRecord` | `OnBreadcrumbCallback#onBreadcrumb` |

## Delivery

| v4.x API  | v5.x API |
| ------------- | ------------- |
| `Delivery#deliver(Report, Configuration)` | `Delivery#deliver(SessionPayload, DeliveryParams)` |
| `Delivery#deliver(SessionTrackingPayload, Configuration)` | `Delivery#deliver(Report, DeliveryParams)` |

## Removed from public API

### MetaData
`MetaData` is no longer publicly accessible, and should be added via the `Bugsnag` interface or via an `OnError` callback.

See the [full documentation](https://docs.bugsnag.com/platforms/android) for more information.

### Report
The `Event` class should now be used to alter the details of an error report. This class is no longer publicly accessible as end-users should not need to set its values

### ErrorReportApiClient/SessionTrackingApiClient
Previously deprecated, use `Delivery` interface instead.

### BadResponseException/DeliveryFailureException/NetworkException
Previously deprecated, use `Delivery` interface instead.

### Notifier
This class is no longer publicly accessible as end-users should not need to set its values.

### BugsnagException
This class is no longer required - you should use your own `Throwable` instead.

### EventReceiver
This class is no longer publicly accessible as end-users should not need to set its values.

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
