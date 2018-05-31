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

You can run the test suite on a device/emulator as follows from within the sdk directory:

```shell
./gradlew connectedCheck
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

If you are a project maintainer, you can build and release a new version of
`bugsnag-android` as follows:

## One-time setup

-   Create a [Sonatype JIRA](https://issues.sonatype.org) account
-   Ask in the [Bugsnag Sonatype JIRA ticket](https://issues.sonatype.org/browse/OSSRH-5533) to become a contributor
-   Ask an existing contributor (likely Simon) to confirm in the ticket
-   Wait for Sonatype them to confirm the approval
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

## Every time

### Pre-release Checklist

- [ ] Does the build pass on the CI server?
- [ ] Are all Docs PRs ready to go?
- [ ] Do the installation instructions work when creating an example app from scratch?
- [ ] Has all new functionality been manually tested on a release build?
  - [ ] Ensure the example app sends an unhandled error
  - [ ] Ensure the example app sends a handled error
  - [ ] If a response is not received from the server, is the report queued for later?
  - [ ] If no network connection is available, is the report queued for later?
  - [ ] On a throttled network, is the request timeout reasonable, and the main thread not blocked?
  - [ ] Are queued reports sent asynchronously?
- [ ] Have the installation instructions been updated on the [dashboard](https://github.com/bugsnag/bugsnag-website/tree/master/app/views/dashboard/projects/install) as well as the [docs site](https://github.com/bugsnag/docs.bugsnag.com)?

### Making the release

- [ ] Update the version number and dex count badge by running `make VERSION=[number] bump`
- [ ] Inspected the updated CHANGELOG, README, and version files to ensure they are correct
- [ ] Release to GitHub, Maven Central, and Bintray by running `make VERSION=[number] release`
  - [ ] "Promote" the release build on Maven Central
    - Go to the [sonatype open source dashboard](https://oss.sonatype.org/index.html#stagingRepositories)
    - Click the search box at the top right, and type “com.bugsnag”
    - Select the com.bugsnag staging repository
    - Click the “close” button in the toolbar, no message
    - Click the “refresh” button
    - Select the com.bugsnag closed repository
    - Click the “release” button in the toolbar
  - [ ] Create a release from your new tag on [GitHub Releases](https://github.com/bugsnag/bugsnag-android/releases)
    - Add the contents of the latest changelog entry to the new release
    - Upload the generated `.aar` file from `build/outputs/aar/bugsnag-android-release.aar`
  - [ ] Open the [Bintray repository](https://bintray.com/bugsnag/maven/bugsnag-android) and publish the new artifacts
- [ ] Merge outstanding docs PRs related to this release


### Post-release Checklist

_(May take some time to propagate to maven central and bintray)_

- [ ] Have all Docs PRs been merged?
- [ ] Can a freshly created example app send an error report from a release build using the released artefact?
- [ ] Do the existing example apps send an error report using the released artifact?
- [ ] Make releases to downstream libraries, if appropriate (generally for bug fixes)
