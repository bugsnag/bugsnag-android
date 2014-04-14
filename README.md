Bugsnag Notifier for Android
============================

The Bugsnag Notifier for Android gives you instant notification of exceptions
thrown from your Android applications.
The notifier hooks into `Thread.UncaughtExceptionHandler`, so any
uncaught exceptions in your app will be sent to your Bugsnag dashboard.

[Bugsnag](https://bugsnag.com) captures errors from your web and mobile
applications, helping you to understand and resolve them as fast as possible.
[Create a free account](https://bugsnag.com) to start capturing exceptions
from your applications.


Installation & Setup
--------------------

-   Download the [latest bugsnag-android.jar](https://s3.amazonaws.com/bugsnagcdn/bugsnag-android/bugsnag-android-2.1.2.jar)
    and place it in your Android app's `libs/` folder.

    *Note: if your project uses [Maven](http://maven.apache.org/) you can
    instead [add bugsnag-android as a dependency](http://mvnrepository.com/artifact/com.bugsnag/bugsnag-android)
    in your `pom.xml`.*

-   Import the `Bugsnag` package in your [Application](http://developer.android.com/reference/android/app/Application.html)
    subclass.

    ```java
    import com.bugsnag.android.*;
    ```

-   In your application's `onCreate` function, register to begin capturing
    exceptions:

    ```java
    Bugsnag.register(this, "your-api-key-goes-here");
    ```

-   Ensure you have the `android.permission.INTERNET` permission listed in
    your `AndroidManifest.xml`.


Recommended: Enable Additional Diagnostic Information
-----------------------------------------------------

-   To track which of your activities were open at the time of any exception,
    you should also have each of your `Activity` classes inherit from
    `BugsnagActivity`:

    ```java
    import com.bugsnag.android.activity.*;
    class MyActivity extends BugsnagActivity { ... }
    ```

    Note: If you are using the
    [Android Support Library](http://developer.android.com/tools/extras/support-library.html),
    [ActionBarSherlock](http://actionbarsherlock.com/) or have a custom base Activity, see
    [Instrumenting Custom Activities](#instrumenting-custom-activities) below.

-   To enable network diagnostics for each device (internet connectivity, etc)
    you should also add the `android.permission.ACCESS_NETWORK_STATE`
    permission to your `AndroidManifest.xml`.


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


Send Non-Fatal Exceptions to Bugsnag
------------------------------------

If you would like to send non-fatal exceptions to Bugsnag, you can pass any
`Throwable` object to the `notify` method:

```java
Bugsnag.notify(new RuntimeException("Non-fatal"));
```

You can also send additional meta-data with this exception:

```java
import com.bugsnag.MetaData;

MetaData metaData = new MetaData();
metaData.addToTab("User", "username", "bob-hoskins");
metaData.addToTab("User", "email", "bob@example.com");

Bugsnag.notify(new RuntimeException("Non-fatal"), metaData);
```


Configuration
-------------

###setContext

Bugsnag uses the concept of "contexts" to help display and group your
errors. Contexts represent what was happening in your application at the
time an error occurs. In an android app, it is useful to set this to be
your currently active `Activity`.

If you are using `BugsnagActivity` then this is set automatically for you.
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

If you have the `android:debuggable="true"` flag set in your
`AndroidManifest.xml`, we'll automatically set this to "development",
otherwise it is set to "production".

###setNotifyReleaseStages

By default, we will notify Bugsnag of exceptions that happen in any
`releaseStage`. If you would like to change which release stages notify
Bugsnag of exceptions you can call `setNotifyReleaseStages`:

```java
Bugsnag.setNotifyReleaseStages("production", "development", "testing");
```

###setAutoNotify

By default, we will automatically notify Bugsnag of any fatal exceptions
in your application. If you want to stop this from happening, you can call
`setAutoNotify`:

```java
Bugsnag.setAutoNotify(false);
```

###setFilters

Sets the strings to filter out from the `extraData` maps before sending
them to Bugsnag. Use this if you want to ensure you don't send
sensitive data such as passwords, and credit card numbers to our
servers. Any keys which contain these strings will be filtered.

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
`Bugsnag.register` from.

###setIgnoreClasses

Sets for which exception classes we should not send exceptions to Bugsnag.

```java
Bugsnag.setIgnoreClasses("java.net.UnknownHostException", "com.example.Custom");
```


Instrumenting Custom Activities
-------------------------------

Bugsnag can add additional diagnostic information to each error by
instrumenting your `Activity` classes.

If you are using `FragmentActivity` from the
[Android Support Library](http://developer.android.com/tools/extras/support-library.html)
your Activities should inherit from `BugsnagFragmentActivity`.

Similarly, if you are using `SherlockActivity` or `SherlockFragmentActivity`
from [ActionBarSherlock](http://actionbarsherlock.com/), your Activities
should inherit from `BugsnagSherlockActivity` or
`BugsnagSherlockFragmentActivity`.

If you have your own custom base `Activity`, you can add the Bugsnag
instrumentation manually by calling `Bugsnag.onActivityPause` in `onPause`,
`Bugsnag.onActivityResume` in `onResume`, `Bugsnag.onActivityCreate` in `onCreate`
and `Bugsnag.onActivityDestroy` in `onDestroy`. Each of these methods take
one paramenter, the activity instance (usually `this`).


Building from Source
--------------------

To build a `.jar` file from source you'll need to use
[Maven](http://maven.apache.org/).

Clone the [bugsnag-android](https://github.com/bugsnag/bugsnag-android)
repository, then run:

```bash
mvn clean package
```

This will generate jar files in the `target` directory.

Building on OSX
---------------

In order to build on OSX, run the following commands,

```bash
cd /System/Library/Java/JavaVirtualMachines/1.6.0.jdk/Contents/Home/lib
sudo ln -s ../../Classes/classes.jar rt.jar
sudo ln -s ../../Classes/jsse.jar .
```


Reporting Bugs or Feature Requests
----------------------------------

Please report any bugs or feature requests on the github issues page for this
project here:

<https://github.com/bugsnag/bugsnag-android/issues>


Contributing
------------

-   [Fork](https://help.github.com/articles/fork-a-repo) the [notifier on github](https://github.com/bugsnag/bugsnag-android)
-   Commit and push until you are happy with your contribution
-   [Make a pull request](https://help.github.com/articles/using-pull-requests)
-   Thanks!


License
-------

The Bugsnag Android notifier is free software released under the MIT License.
See [LICENSE.txt](https://github.com/bugsnag/bugsnag-android/blob/master/LICENSE.txt) for details.
