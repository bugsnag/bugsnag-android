Bugsnag Notifier for Android
============================

[Bugsnag](https://bugsnag.com) for Android automatically detects crashes in
your Android apps, collecting diagnostic information and immediately notifying
your development team.

[Create a free account](https://bugsnag.com) to start capturing exceptions
from your applications.


Installation
------------

### Using Android Studio or Gradle

Add `bugsnag-android` to the `dependencies` section in your `build.gradle`:

```gradle
compile 'com.bugsnag:bugsnag-android:+'
```

### Using Maven

Add `bugsnag-android` as a dependency in your `pom.xml`:

```xml
<dependency>
    <groupId>com.bugsnag</groupId>
    <artifactId>bugsnag-android</artifactId>
    <version>LATEST</version>
</dependency>
```

### Using a Jar

-   Download the [latest bugsnag-android.jar](https://github.com/bugsnag/bugsnag-android/releases/latest)
-   Place it in your Android app's `libs/` folder


Configuring Your Manifest
-------------------------

-   Ensure you have the `android.permission.INTERNET` permission listed in
    your `AndroidManifest.xml`. This is required to send crash reports to
    Bugsnag

-   To enable network diagnostics for each device (internet connectivity, etc)
    you should also add the `android.permission.ACCESS_NETWORK_STATE`
    permission to your `AndroidManifest.xml`.

-   To see which activity was active at the time of a crash, you should also
    add the `android.permission.GET_TASKS` permission to your
    `AndroidManifest.xml`. If you are targeting API level 21+ (Android 5.0+)
    this is not required.


Initializing Bugsnag
--------------------

-   Import the `Bugsnag` package in your [Application](http://developer.android.com/reference/android/app/Application.html)
    subclass:

    ```java
    import com.bugsnag.android.*;
    ```

-   In your application's `onCreate` function, initialize Bugsnag to begin
    capturing exceptions:

    ```java
    Bugsnag.init(this, "your-api-key-goes-here");
    ```


Sending Custom Data With Exceptions
-----------------------------------

It is often useful to send additional meta-data about your app, such as
information about the currently logged in user, along with any exceptions,
to help debug problems. To add custom data to every exception you can
use `addToTab`:

```java
Bugsnag.addToTab("User", "Name", "Bob Hoskins");
Bugsnag.addToTab("User", "Paying Customer?", true);
```

You can also add custom data or modify error information before each exception
is sent to Bugsnag using `BeforeNotify` callbacks. See
[beforeNotify](#beforenotify) below for details.


Logging Breadcrumbs
-------------------

Bugsnag allows you to leave developer-defined log messages called "breadcrumbs"
to help understand exactly what was happening in your application in the time
before each crash.

When logging a breadcrumb, we'll keep track of the timestamp associated with
the log message, and show both the message and timestamp on your dashboard.

To leave breadcrumbs, you can use `leaveBreadcrumb`:

```java
Bugsnag.leaveBreadcrumb("App loaded");
Bugsnag.leaveBreadcrumb("User clicked a button");
```

By default, we'll store and send the last 20 breadcrumbs you leave before
errors are sent to Bugsnag. If you'd like to increase this number, you can
call `setMaxBreadcrumbs`:

```java
Bugsnag.setMaxBreadcrumbs(50);
```


Sending Handled Exceptions
--------------------------

If you would like to send non-fatal exceptions to Bugsnag, you can pass any
`Throwable` object to the `notify` method:

```java
Bugsnag.notify(new Exception("Non-fatal"));
```

### With Custom Data

You can also send additional meta-data with this exception:

```java
import com.bugsnag.android.MetaData;

MetaData metaData = new MetaData();
metaData.addToTab("User", "username", "bob-hoskins");
metaData.addToTab("User", "email", "bob@example.com");

Bugsnag.notify(new Exception("Non-fatal"), metaData);
```

### With a Severity

You can set the severity of an error in Bugsnag by including the severity option when
notifying bugsnag of the error,

```java
import com.bugsnag.android.Severity;

Bugsnag.notify(new Exception("Non-fatal"), Severity.INFO)
```

Valid severities are `Severity.ERROR`, `Severity.WARNING` and `Severity.INFO`.

Severity is displayed in the dashboard and can be used to filter the error list.
By default all crashes (or unhandled exceptions) are set to `Bugsnag.ERROR` and all
`Bugsnag.notify` calls default to `Bugsnag.WARNING`.


### With Custom Data and Severity

You can send handled exceptions with both custom data and severity as follows:

```java
import com.bugsnag.android.*;

MetaData metaData = new MetaData();
metaData.addToTab("User", "username", "bob-hoskins");

Bugsnag.notify(new Exception("Non-fatal"), Severity.INFO, metaData);
```


Configuration
-------------

###setContext

Bugsnag uses the concept of "contexts" to help display and group your
errors. Contexts represent what was happening in your application at the
time an error occurs. In an android app, it is useful to set this to be
your currently active `Activity`.

If you enable the `GET_TASKS` permission, then this is set automatically for you.
If you would like to set the bugsnag context manually, you can call
`setContext`:

```java
Bugsnag.setContext("MyActivity");
```

###setUser

Bugsnag helps you understand how many of your users are affected by each
error. In order to do this, we need to send along user information with every
exception.

If you would like to enable this, set the `user`. You can set the user id,
which should be the unique id to represent that user across all your apps,
the user's email address and the user's name:

```java
Bugsnag.setUser("userId", "user@email.com", "User Name");
```

###setReleaseStage

If you would like to distinguish between errors that happen in different
stages of the application release process (development, production, etc)
you can set the `releaseStage` that is reported to Bugsnag.

```java
Bugsnag.setReleaseStage("testing");
```

If you are running a debug build, we'll automatically set this to "development",
otherwise it is set to "production".

###setNotifyReleaseStages

By default, we will notify Bugsnag of exceptions that happen in any
`releaseStage`. If you would like to change which release stages notify
Bugsnag of exceptions you can call `setNotifyReleaseStages`:

```java
Bugsnag.setNotifyReleaseStages("production", "development", "testing");
```

###setFilters

Sets which values should be removed from any `MetaData` objects before sending
them to Bugsnag. Use this if you want to ensure you don't send sensitive data
such as passwords, and credit card numbers to our servers. Any keys which
contain these strings will be filtered.

```java
Bugsnag.setFilters(new String[]{"password", "credit_card_number"});
```

By default, `filters` is set to `new String[] {"password"};`

###setProjectPackages

Sets which package names Bugsnag should consider as "inProject". We mark
stacktrace lines as in-project if they originate from any of these
packages.

```java
Bugsnag.setProjectPackages("com.company.package1", "com.company.package2");
```

By default, `projectPackages` is set to be the package you called
`Bugsnag.init` from.

###setIgnoreClasses

Sets for which exception classes we should not send exceptions to Bugsnag.

```java
Bugsnag.setIgnoreClasses("java.net.UnknownHostException", "com.example.Custom");
```

###setAppVersion

We'll automatically pull your app version from the `versionName` field in
your `AndroidManifest.xml` file. If you'd like to override this you can call
`setAppVersion`:

```java
Bugsnag.setAppVersion("1.0.0-alpha");
```

> Note: Bugsnag uses [Semantic Versioning](http://semver.org/) for app version
sorting and filtering on the Bugsnag dashboard.

###setSendThreads

Sets if we should collect and send thread state along with errors.

Bt default `sendThreads` is set to `true`.

```java
Bugsnag.setSendThreads(false);
```

###setEndpoint

Set the endpoint to send data to. By default we'll send reports to our
standard `https://notify.bugsnag.com` endpoint, but you can override this if
you are using [Bugsnag Enterprise](https://bugsnag.com/enterprise), to point
to your own Bugsnag endpoint:

```java
Bugsnag.setEndpoint("http://bugsnag.internal.example.com");
```

###beforeNotify

Add a "before notify" callback, to execute code before every notification to
Bugsnag.

You can use this to add or modify information attached to an error before it
is sent to your dashboard. You can also return `false` from any callback to
halt execution.

```java
Bugsnag.beforeNotify(new BeforeNotify() {
    public boolean run(Error error) {
        error.setSeverity(Severity.INFO);
        return true;
    }
});
```


Reporting Bugs or Feature Requests
----------------------------------

Please report any bugs or feature requests on the github issues page for this
project here:

<https://github.com/bugsnag/bugsnag-android/issues>


Contributing
------------

We'd love to see your contributions! For information on how to build, test
and releease `bugsnag-android`, see our [contributing guide](CONTRIBUTING.md).


License
-------

The Bugsnag Android notifier is free software released under the MIT License.
See [LICENSE.txt](https://github.com/bugsnag/bugsnag-android/blob/master/LICENSE.txt)
for details.
