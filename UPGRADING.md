Upgrading Guide
===============

Upgrade from 2.0 to 3.0
-----------------------
-   Change any `Bugsnag.register` calls to `Bugsnag.init`:

    ```java
    // Old
    Bugsnag.register("api-key");

    // New
    Bugsnag.init("api-key");
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
