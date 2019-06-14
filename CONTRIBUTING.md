Contributing
============

-   [Fork](https://help.github.com/articles/fork-a-repo) the [notifier on github](https://github.com/bugsnag/bugsnag-android)
-   Build and test your changes
-   Commit and push until you are happy with your contribution
-   [Make a pull request](https://help.github.com/articles/using-pull-requests)
-   Thanks!

## Reporting issues

Are you having trouble getting started? Please [contact us directly](mailto:support@bugsnag.com?subject=%5BGitHub%5D%20Android%20SDK%20-%20having%20trouble%20getting%20started%20with%20Bugsnag) for assistance with integrating Bugsnag into your application.
If you have spotted a problem with this module, feel free to open a [new issue](https://github.com/bugsnag/bugsnag-android/issues/new). Here are a few things to check before doing so:

* Are you using the latest version of `bugsnag-android`? If not, does updating to the latest version fix your issue?
* Has somebody else [already reported](https://github.com/bugsnag/bugsnag-android/issues?utf8=%E2%9C%93&q=is%3Aissue%20is%3Aopen) your issue? Feel free to add additional context to or check-in on an existing issue that matches your own.
* Is your issue caused by this module? Only things related to the `bugsnag-android` module should be reported here. For anything else, please [contact us directly](mailto:support@bugsnag.com) and we'd be happy to help you out.

### Fixing issues

If you've identified a fix to a new or existing issue, we welcome contributions!
Here are some helpful suggestions on contributing that help us merge your PR quickly and smoothly:

* [Fork](https://help.github.com/articles/fork-a-repo) the
  [library on GitHub](https://github.com/bugsnag/bugsnag-android)
* Build and test your changes using the example app and test suite
* Commit and push until you are happy with your contribution
* [Make a pull request](https://help.github.com/articles/using-pull-requests)
* Ensure the automated checks pass (and if it fails, please try to address the cause)

### Adding features

Unfortunately we’re unable to accept PRs that add features or refactor the library at this time.
However, we’re very eager and welcome to hearing feedback about the library so please contact us directly to discuss your idea, or open a
[feature request](https://github.com/bugsnag/bugsnag-android/issues/new?template=Feature_request.md) to help us improve the library.

Here’s a bit about our process designing and building the Bugsnag libraries:

* We have an internal roadmap to plan out the features we build, and sometimes we will already be planning your suggested feature!
* Our open source libraries span many languages and frameworks so we strive to ensure they are idiomatic on the given platform, but also consistent in terminology between platforms. That way the core concepts are familiar whether you adopt Bugsnag for one platform or many.
* Finally, one of our goals is to ensure our libraries work reliably, even in crashy, multi-threaded environments. Oftentimes, this requires an intensive engineering design and code review process that adheres to our style and linting guidelines.

### Updating dependencies

Most dependencies are controlled by the module-level gradle files, however
running the NDK C/C++ components also depends on
[`greatest`](https://github.com/silentbicycle/greatest) and [`parson`](https://github.com/kgabis/parson), managed by [clib](https://github.com/clibs/clib).
Both libraries are vendored into the repository and clib is not required unless
updating the dependencies.

To update a clib dependency, reinstall it. For example, using parson:

    clib install kgabis/parson -o src/test/cpp/deps --save


## Installing the Android SDK

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


## Building the Library

Be sure to clone the repository submodules:

```shell
git submodule update --init --recursive
```

You can build new `.aar` files as follows:

```shell
./gradlew sdk:assemble
```

Files are generated into`sdk/build/outputs/aar`.

### Building with custom ABIs

To build the NDK module with specific ABIs, use the `ABI_FILTERS` project
option:

```shell
./gradlew assemble -PABI_FILTERS=x86,arm64-v8a
```

## Running Tests Locally

Running the test suite requires a connected android device or emulator.

### Unit tests

You can run the test suite on a device/emulator as follows from within the sdk directory:

```shell
./gradlew connectedCheck
```

### End-to-end tests

To run the end-to-end tests, first set up the environment by running
[Bundler](https://bundler.io):

```shell
bundle install
```

The tests require two environment variables to be set:

* `ANDROID_HOME`, set the the location of the Android SDK
* `ANDROID_EMULATOR`, set to the name of an installed emulator

Then run the tests using:

```shell
bundle exec maze-runner
```

### Running Lint

You can run lint on the project using the following command:

```shell
./gradlew lint checkstyle detekt
```

## Running remote tests

Remote tests can be run against real devices provided by BrowserStack. In order to run these tests, you need:

A BrowserStack App Automate Username: `BROWSER_STACK_USERNAME`
A BrowserStack App Automate Access Key: `BROWSER_STACK_ACCESS_KEY`
A local docker and docker-compose installation.

### Instrumentation tests

Ensure that the following environment variables are set:

* `BROWSER_STACK_USERNAME`: The BrowserStack App Automate Username
* `BROWSER_STACK_ACCESS_KEY`: The BrowserStack App Automate Access Key
* `NDK_VERSION`: The version of NDK that should be used to build the app

Run `make remote-test`

### End-to-end tests

Ensure that the following environment variables are set:

* `BROWSER_STACK_USERNAME`: The BrowserStack App Automate Username
* `BROWSER_STACK_ACCESS_KEY`: The BrowserStack App Automate Access Key
* `DEVICE_TYPE`: The android version to run the tests against, one of: ANDROID_5, ANDROID_6, ANDROID_7, ANDROID_8, ANDROID_9

Run `make remote-integration-tests`

## Building the Example App

You can build and install the example app to as follows:

```shell
./gradlew installDebug
```

This builds the latest version of the library and installs an app onto your
device/emulator.

## Installing/testing against a local maven repository

Sometimes its helpful to build and install the bugsnag-android libraries into a
local repository and test the entire dependency flow inside of a sample
application.

To get started:

1. In the `bugsnag-android` directory, run
   `./gradlew assembleRelease publishSDKPublicationToMavenLocal && ./gradlew assembleRelease publishSDKPublicationToMavenLocal -PreleaseNdkArtefact=true`.
   This installs `bugsnag-android` and `bugsnag-android-ndk` into your local
   maven repository.
2. In your sample application `build.gradle`, add `mavenLocal()` to the *top* of
   your `allprojects` repositories section:

   ```groovy
   allprojects {
     repositories {
       mavenLocal()
       // other repos as needed
     }
   }
   ```
3. In your sample application `app/build.gradle`, add the following to the
   dependencies section, inserting the exact version number required:

   ```groovy
   dependencies {
     implementation 'com.bugsnag:bugsnag-android-ndk:[VERSION NUMBER]'
   }
   ```
4. Clean your sample application and reload dependencies *every time* you
   rebuild/republish the local dependencies:

   ```
   ./gradlew clean --refresh-dependencies
   ```

# Releasing a New Version

If you are a project maintainer, you can build and release a new version of
`bugsnag-android` as follows:

## One-time setup

-   Create a [Bintray](https://bintray.com/signup/oss) account, and ask a Bugsnag admin to add you to the organisation
-   Create a [Sonatype JIRA](https://issues.sonatype.org) account
-   Ask in the [Bugsnag Sonatype JIRA ticket](https://issues.sonatype.org/browse/OSSRH-5533) to become a contributor
-   Ask an existing contributor (likely Simon) to confirm in the ticket
-   Wait for Sonatype them to confirm the approval
-   Create a file `~/.gradle/gradle.properties` with the following contents:

    ```ini
    # Your credentials for https://oss.sonatype.org/
    # NOTE: An equals sign (`=`) in any of these fields will break the parser
    # NOTE: Do not wrap any field in quotes

    NEXUS_USERNAME=your-nexus-username
    NEXUS_PASSWORD=your-nexus-password
    nexusUsername=your-nexus-username
    nexusPassword=your-nexus-password

    # Your credentials for Bintray
    # From https://bintray.com/profile
    bintray_user=your-bintray-username
    # Get your Bintray API key from https://bintray.com/profile/edit > API Key
    bintray_api_key=your-api-key

    # GPG key details
    # Your key must be added to a public key server, such as http://keys.gnupg.net:
    # 1. Get your key id by running `gpg --list-keys --keyid-format=short`. It
    #    should be 8-character hexadecimal.
    # 2. Export your key using `gpg --armor --export <key-id>`
    # 3. Upload to a server using `gpg --keyserver hkp://keys.gnupg.net --send-keys <key-id>`
    signing.keyId=<key-id>
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
  - [ ] On a throttled network, is the request timeout reasonable, and the main thread not blocked by any visible UI freeze? (Throttling can be achieved by setting both endpoints to "https://httpstat.us/200?sleep=5000")
  - [ ] Are queued reports sent asynchronously?
- Native functionality checks:
  - [ ] Rotate the device before notifying. Is the orientation at the time
    persisted in the report on the dashboard?
  - [ ] Rotate the device before causing a native crash. Is the orientation at
    the time of the crash persisted in the report on the dashboard?
  - [ ] Wait a few seconds before a native crash. Does the reported duration in
    foreground match your expectation? Is the value for "inForeground" correct?
  - [ ] Do the function names demangle correctly when using notify?
- [ ] Have the installation instructions been updated on the [dashboard](https://github.com/bugsnag/dashboard-js/tree/master/js/dashboard/components/integration_instructions) as well as the [docs site](https://github.com/bugsnag/docs.bugsnag.com)?
- [ ] Do the installation instructions work for a manual integration?

### Making the release

- Make a PR to release the following changes to master, creating a release
  branch from the "next" branch if this is a feature release:
  - [ ] Update the version number and dex count badge by running `make VERSION=[number] bump`
  - [ ] Inspect the updated CHANGELOG, README, and version files to ensure they are correct
- Once merged:
  - Pull the latest changes (checking out master if necessary) and build by running `./gradlew sdk:assembleRelease`
  - Release to GitHub:
    - [ ] Run `git tag vX.X.X && git push origin --tags`
    - [ ] Create a release from your new tag on [GitHub Releases](https://github.com/bugsnag/bugsnag-android/releases)
    - [ ] Upload the generated `.aar` file from `{sdk,ndk}/build/outputs/aar/bugsnag-android-*.aar`
  - [ ] Release to Maven Central and Bintray by running `./gradlew sdk:assembleRelease publish bintrayUpload && ./gradlew sdk:assembleRelease publish bintrayUpload -PreleaseNdkArtefact=true`
  - [ ] "Promote" the release build on Maven Central:
    - Go to the [sonatype open source dashboard](https://oss.sonatype.org/index.html#stagingRepositories)
    - Click the search box at the top right, and type “com.bugsnag”
    - Select the com.bugsnag staging repository
    - Click the “close” button in the toolbar, no message
    - Click the “refresh” button
    - Select the com.bugsnag closed repository
    - Click the “release” button in the toolbar
  - Open the Bintray repositories and publish the new artifacts:
    - [ ] [SDK repo](https://bintray.com/bugsnag/maven/bugsnag-android/_latestVersion)
    - [ ] [NDK repo](https://bintray.com/bugsnag/maven/bugsnag-android-ndk/_latestVersion)
- Merge outstanding docs PRs related to this release


### Post-release Checklist

_(May take some time to propagate to maven central and bintray)_

- [ ] Have all Docs PRs been merged?
- [ ] Can a freshly created example app send an error report from a release build using the released artefact?
- [ ] Do the existing example apps send an error report using the released artifact?
- [ ] Make releases to downstream libraries, if appropriate (generally for bug fixes)
