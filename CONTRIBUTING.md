Contributing
============

-   [Fork](https://help.github.com/articles/fork-a-repo) the [notifier on github](https://github.com/bugsnag/bugsnag-android)
-   Build and test your changes
-   Commit and push until you are happy with your contribution
-   [Make a pull request](https://help.github.com/articles/using-pull-requests)
-   Thanks!

Running `./gradlew` installs both the gradle build system and the Android SDK,
but you'll need to make sure that the `adb` tool installed as part of the
Android SDK is available in your `$PATH` before building, eg:

```
export PATH=$PATH:~/.android-sdk/platform-tools
```


Building the Libarary
---------------------

You can build new `.jar` and `.aar` files as follows:

```shell
./gradlew clean :build
```

Jar files are generated into `build/outputs/jar` and Aar files are generated into
`build/outputs/aar`.


Building the Example App
------------------------

You can build and install the example app to as follows:

```shell
./gradlew clean example:installDebug
```

This builds the latest version of the library and installs an app onto your
device/emulator.


Running Tests
-------------

Running the test suite requires a connected android device or emulator.

You can run the test suite on a device/emulator as follows:

```shell
./gradlew clean :connectedCheck
```


Releasing a New Version
-----------------------

If you are a project maintainer, you can build and release a new version of
`bugsnag-android` as follows:

### 1. Prepare for release

-   Update the `CHANGELOG` and `README.md` with any new features

-   Update the version numbers in `gradle.properties` and `src/main/java/com/bugsnag/android/Notifier.java`

-   Commit and tag the release

    ```shell
    git commit -am "v3.x.x"
    git tag v3.x.x
    git push origin master && git push --tags
    ```

### 2. Release to Maven Central

-   Create a file `~/.gradle/gradle.properties` with the following contents:

    ```ini
    # Your credentials for https://oss.sonatype.org/
    NEXUS_USERNAME=your-nexus-username
    NEXUS_PASSWORD=your-nexus-password

    # GPG key details
    signing.keyId=your-gpg-key-id # From gpg --list-keys
    signing.password=your-gpg-key-passphrase
    signing.secretKeyRingFile=/Users/james/.gnupg/secring.gpg
    ```

-   Build and upload the new version

    ```shell
    ./gradlew clean :uploadArchives
    ```

-   "Promote" the release build on Maven Central

    -   Go to the [sonatype open source dashboard](https://oss.sonatype.org/index.html#stagingRepositories)
    -   Click “Staging Repositories”
    -   Click the search box at the top right, and type “com.bugsnag”
    -   Select the com.bugsnag staging repository
    -   Click the “close” button in the toolbar, no message
    -   Click the “refresh” button
    -   Select the com.bugsnag closed repository
    -   Click the “release” button in the toolbar

### 3. Upload the .jar file to GitHub

-   Create a "release" from your new tag on [GitHub Releases](https://github.com/bugsnag/bugsnag-android/releases)
-   Upload the generated `.jar` file from `build/outputs/jar/bugsnag-android-x.y.z.jar` on the "edit tag" page for this release tag
