Bugsnag Notifier for Android
============================

The Bugsnag Notifier for Android gives you instant notification of exceptions
thrown from your Android applications.
The notifier hooks into `Thread.UncaughtExceptionHandler`, which means any
uncaught exceptions will trigger a notification to be sent to your Bugsnag
project.

[Bugsnag](http://bugsnag.com) captures errors in real-time from your web,
mobile and desktop applications, helping you to understand and resolve them
as fast as possible. [Create a free account](http://bugsnag.com) to start
capturing exceptions from your applications.


Installation & Setup
--------------------

-   [Download the latest bugsnag-android.jar file](TODO) and place it in your app's
    classpath.

    *Note: if your project uses [Maven](http://maven.apache.org/) you can 
    instead [add bugsnag-android as a dependency](http://mvnrepository.com/artifact/com.bugsnag/bugsnag-android)
    in your pom.xml.*

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

    For additional diagnostic information, add the
    `android.permission.ACCESS_NETWORK_STATE` permission to your
    `AndroidManifest.xml`.


Recommended: Inherit from BugsnagActivity
-----------------------------------------

To have additional diagnostic information, you should also have each of your 
`Activity` classes inherit from `BugsnagActivity`.
This will track which of your activities were open at the time of
any exception, and present them in your Bugsnag error dashboard.

```java
class MyActivity extends BugsnagActivity {
    ...
}
```


Send Non-Fatal Exceptions to Bugsnag
------------------------------------

If you would like to send non-fatal exceptions to Bugsnag, you can pass any
`Throwable` object to the `notify` method:

```java
Bugsnag.notify(new RuntimeException("Non-fatal"));
```

You can also send additional meta-data with your exception:

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

###setUserId

Bugsnag helps you understand how many of your users are affected by each
error. In order to do this, we send along a userId with every exception.
By default we will generate a unique ID and send this ID along with every
exception from an individual device.

If you would like to override this `userId`, for example to set it to be a
username of your currently logged in user, you can call `setUserId`:

```java
Bugsnag.setUserId("leeroy-jenkins");
```

###setReleaseStage

If you would like to distinguish between errors that happen in different
stages of the application release process (development, production, etc)
you can set the `releaseStage` that is reported to Bugsnag.

```java
Bugsnag.setReleaseStage("development");
```

By default this is set to be "production".

###setNotifyReleaseStages

By default, we will only notify Bugsnag of exceptions that happen when
your `releaseStage` is set to be "production". If you would like to
change which release stages notify Bugsnag of exceptions you can
call `setNotifyReleaseStages`:

```java
Bugsnag.setNotifyReleaseStages(new String[]{"production", "development"});
```

###setAutoNotify

By default, we will automatically notify Bugsnag of any fatal exceptions
in your application. If you want to stop this from happening, you can call
`setAutoNotify`:

```java
Bugsnag.setAutoNotify(false);
```

###setExtraData

It if often very useful to send some extra application or user specific
data along with every exception. To do this, you can call the
`setExtraData` method:

```java
Map<String,String> extraData = new HashMap<String,String>();
extraData.put("username", "bob-hoskins");
extraData.put("registered_user", "yes");

Bugsnag.setExtraData(extraData);
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
bugsnag.setProjectPackages("com.company.package1", "com.company.package2");
```

By default, `projectPackages` is set to be the package you called
`Bugsnag.register` from.


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
