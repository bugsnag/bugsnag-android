Contributing
============

-   [Fork](https://help.github.com/articles/fork-a-repo) the [notifier on github](https://github.com/bugsnag/bugsnag-android)
-   Build and test your changes
-   Commit and push until you are happy with your contribution
-   [Make a pull request](https://help.github.com/articles/using-pull-requests)
-   Thanks!

Building the Libarary
---------------------

You can build new `.jar` and `.aar` files as follows:

```shell
./gradlew :build
```

Jar files are generated into `build/libs` and Aar files are generated into
`build/outputs/aar`.


Building the Example App
------------------------

You can build and install the example app to as follows:

```shell
./gradlew example:installDebug
```

This builds the latest version of the library and installs an app onto your
device/emulator.


Running Tests
-------------

Running the test suite requires a connected android device or emulator.

You can run the test suite on a device/emulator as follows:

```shell
./gradlew :connectedCheck
```


Releasing to Maven Central
--------------------------

If you are a project maintainer, you can build and release a new version of
bugsnag-android to maven central as follows:

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

-   Update the version numbers in `gradle.properties` and
    `src/main/java/com/bugsnag/android/Notifier.java`.

    Append `-SNAPSHOT` to the version number if performing a test build.

-   Build and upload the new version

    ```shell
    ./gradlew :uploadArchives
    ```
