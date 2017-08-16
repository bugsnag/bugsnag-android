Upgrading Guide
===============

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
