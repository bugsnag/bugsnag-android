# Releasing a new version

`bugsnag-android` is released via [Sonatype](https://oss.sonatype.org/). If you are a project maintainer you can release a new version by unblocking the publish step on CI and following the steps below.

## Pre-release checklist

This contains a prompt of checks which you may want to test, depending on the extent of the changeset:

- [ ] Has the full test suite been triggered on Buildkite and does it pass?
- [ ] Have versions of Android not covered by CI been considered?
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

## Making the release

- Check the performance benchmarks against the [baseline](BENCHMARKS.md) to confirm there are no serious regressions
- Create a new release branch from `next` -> `release/vN.N.N`
- Pull the release branch and update it locally:
  - [ ] Update the version number with `make VERSION=[number] bump`
  - [ ] Inspect the updated CHANGELOG, README, and version files to ensure they are correct
- Open a Pull Request from the release branch to `master`
- Once merged:
  - Pull the latest changes (checking out `master` if necessary)
  - On CI:
    - Trigger the release step by allowing the `Trigger package publish` step to continue
    - Verify the `Publish` step runs correctly and the artefacts are upload to sonatype.
  - Release to GitHub:
    - [ ] Create *and tag* the release from `master` on [GitHub Releases](https://github.com/bugsnag/bugsnag-android/releases)
  - Checkout `master` and pull the latest changes
  - [ ] Test the Sonatype artefacts in the example app by adding the newly created 'combugsnag-XXXX' repository to the build.gradle:  `maven {url "https://oss.sonatype.org/service/local/repositories/combugsnag-XXXX/content/"}`
  - [ ] "Promote" the release build on Maven Central:
    - Go to the [sonatype open source dashboard](https://oss.sonatype.org/index.html#stagingRepositories)
    - Click the search box at the top right, and type “com.bugsnag”
    - Select the com.bugsnag staging repository
    - Ensure that AARs and POMs are present for each module, and that ProGuard rules are present for AARs which define ProGuard rules
    - Click the “close” button in the toolbar, no message
    - Click the “refresh” button
    - Select the com.bugsnag closed repository
    - Click the “release” button in the toolbar
  - Merge outstanding docs PRs related to this release
  - Raise PRs to update the bugsnag-android dependency for [bugsnag-js](https://github.com/bugsnag/bugsnag-js), [bugsnag-unity](https://github.com/bugsnag/bugsnag-unity), [bugsnag-flutter](https://github.com/bugsnag/bugsnag-flutter) and [bugsnag-unreal](https://github.com/bugsnag/bugsnag-unreal)
    - Also consider a PR for [bugsnag-cocos2dx](https://github.com/bugsnag/bugsnag-cocos2dx) if there is a critical fix

## Post-release checklist

_(May take some time to propagate to maven central)_

- [ ] Have all Docs PRs been merged?
- [ ] Can a freshly created example app send an error report from a release build using the released artefact?
- [ ] Do the existing example apps send an error report using the released artifact?
- [ ] Make releases to downstream libraries, if appropriate (generally for critical bug fixes)

### Manual publishing

Manual publishing is discouraged, but is possible in exceptional circumstances by running `./gradlew assembleRelease publish`. This also requires creating a GPG key and registering an account with Sonatype.

### Creating a GPG key

-   Create a GPG key if you haven't got one already (`gpg --full-gen-key` and select RSA/4096bit). The build system requires a GPG key ring set up using GPG 1.x, but many systems now ship with GPG 2.x. As a workaround, after creating your key you can manually create the `secring.gpg` file by running `gpg --export-secret-keys >~/.gnupg/secring.gpg`
-   Create a [Sonatype JIRA](https://issues.sonatype.org) account
-   Ask in the [Bugsnag Sonatype JIRA ticket](https://issues.sonatype.org/browse/OSSRH-5533) to become a contributor
-   Ask an existing contributor (likely Simon) to confirm in the ticket
-   Wait for Sonatype to confirm the approval
-   Create a file `~/.gradle/gradle.properties` with the following contents:

    ```ini
    # Your credentials for https://oss.sonatype.org/
    # NOTE: An equals sign (`=`) in any of these fields will break the parser
    # NOTE: Do not wrap any field in quotes

    NEXUS_USERNAME=your-nexus-username
    NEXUS_PASSWORD=your-nexus-password
    nexusUsername=your-nexus-username
    nexusPassword=your-nexus-password

    # GPG key details
    # See https://central.sonatype.org/publish/requirements/gpg/ for full documentation
    # Your key must be added to a public key server, such as http://keyserver.ubuntu.com:
    # 1. Get your key id by running `gpg --list-keys --keyid-format=short`. It
    #    should be 8-character hexadecimal.
    # 2. Export your key using `gpg --armor --export <key-id>`
    # 3. Distribute your public key: `gpg --keyserver keyserver.ubuntu.com --send-keys`
    signing.keyId=<key-id>
    signing.password=your-gpg-key-passphrase
    signing.secretKeyRingFile=/Users/{username}/.gnupg/secring.gpg
    ```
