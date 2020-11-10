# bugsnag-plugin-android-ndk

This module detects NDK signals/exceptions and reports them to bugsnag.

## High-level Overview

This module installs C signal handlers and a CPP exception handler. When a native crash occurs,
it writes a report to disk. This is then converted to a JVM report on the next app launch, and is
delivered to the error reporting API.

## Updating native dependencies

Most dependencies are controlled by the module-level gradle files, however
running the NDK C/C++ components also depends on
[`greatest`](https://github.com/silentbicycle/greatest) and [`parson`](https://github.com/kgabis/parson), managed by [clib](https://github.com/clibs/clib).
Both libraries are vendored into the repository and clib is not required unless
updating the dependencies.

To update a clib dependency, reinstall it. For example, using parson:

    clib install kgabis/parson -o src/test/cpp/deps --save
