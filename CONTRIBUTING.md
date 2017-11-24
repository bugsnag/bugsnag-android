Contributing
============

-   [Fork](https://help.github.com/articles/fork-a-repo) the [notifier on github](https://github.com/bugsnag/bugsnag-android)
-   Build and test your changes
-   Commit and push until you are happy with your contribution
-   [Make a pull request](https://help.github.com/articles/using-pull-requests)
-   Thanks!


Installing the Android SDK
--------------------------

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


Building the Library
---------------------

You can build new `.aar` files as follows:

```shell
./gradlew clean :build
```

Files are generated into`build/outputs/aar`.


Running Tests
-------------

Running the test suite requires a connected android device or emulator.

You can run the test suite on a device/emulator as follows:

```shell
./gradlew clean :connectedCheck
```

Running Lint
------------
You can run lint on the project using the following command:

```shell
./gradlew lint
```

Building the Example App
------------------------

You can build and install the example app to as follows:

```shell
./gradlew clean example:installJavaExampleDebug
```

This builds the latest version of the library and installs an app onto your
device/emulator.


Releasing a New Version
-----------------------

## Release Checklist
Please follow the testing instructions in [the platforms release checklist](https://github.com/bugsnag/platforms-release-checklist/blob/master/README.md), and any additional steps directly below.

### Instructions

If you are a project maintainer, you can build and release a new version of
`bugsnag-android` as follows:

### 1. Ensure you have permission to make a release

This process is a little ridiculous...

-   Create a [Sonatype JIRA](https://issues.sonatype.org) account
-   Ask in the [Bugsnag Sonatype JIRA ticket](https://issues.sonatype.org/browse/OSSRH-5533) to become a contributor
-   Ask an existing contributor (likely Simon) to confirm in the ticket
-   Wait for Sonatype them to confirm the approval


### 2. Prepare for release

-   Test unhandled and handled exception reporting via the example application,
    ensuring both kinds of reports are sent.
-   Update the `CHANGELOG` and `README.md` with any new features

### 3. Release to Maven Central

-   Create a file `~/.gradle/gradle.properties` with the following contents:

    ```ini
    # Your credentials for https://oss.sonatype.org/
    # NOTE: An equals sign (`=`) in any of these fields will break the parser
    NEXUS_USERNAME=your-nexus-username
    NEXUS_PASSWORD=your-nexus-password

    # GPG key details
    signing.keyId=your-gpg-key-id # From gpg --list-keys
    signing.password=your-gpg-key-passphrase
    signing.secretKeyRingFile=/Users/{username}/.gnupg/secring.gpg
    ```

-   Build and upload the new version

-   Update the version number, tag a release, and upload an archive by running `make VERSION=[number] release`

-   "Promote" the release build on Maven Central

    -   Go to the [sonatype open source dashboard](https://oss.sonatype.org/index.html#stagingRepositories)
    -   Click the search box at the top right, and type “com.bugsnag”
    -   Select the com.bugsnag staging repository
    -   Click the “close” button in the toolbar, no message
    -   Click the “refresh” button
    -   Select the com.bugsnag closed repository
    -   Click the “release” button in the toolbar

### 4. Upload the .aar file to GitHub

-   Create a "release" from your new tag on [GitHub Releases](https://github.com/bugsnag/bugsnag-android/releases)
-   Upload the generated `.aar` file from `build/outputs/aar/bugsnag-android-release.aar` on the "edit tag" page for this release tag

### 5. Update documentation

-    Update installation instructions in the quickstart
     guides on the website with any new content (in `_android.slim`)
-    Bump the version number in the installation instructions on
     docs.bugsnag.com/platforms/android, and add any new content

### 6. Keep dependent libraries in sync

-    Make releases to downstream libraries, if appropriate (generally for bug
     fixes)
     
### 7. Update Method Count Badge
-   Update the version number specified in the URL for the method count badge in the README. 
