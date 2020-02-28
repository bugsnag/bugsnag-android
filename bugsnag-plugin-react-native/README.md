# bugsnag-plugin-react-native

This module detects errors in React Native apps and reports them to bugsnag.

## High-level Overview

This module depends on bugsnag-android-core, and implements a `BugsnagReactNativePlugin` which is
added in the native layer using the `Plugin` interface. This class is intended to be the sole
internal API which `BugsnagReactNative` in the `bugsnag-js` repository invokes.

The responsibility of this module is to serialize information into a format that can be understood
by the React Bridge, and to forward method calls onto the appropriate site from `bugsnag-js`.

The responsibility of Android code in the `bugsnag-js` package is to implement any functionality
that requires the React Native Android dependency. A minimal number of classes require this and
 by default code should be implemented in this module.
