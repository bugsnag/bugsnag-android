# bugsnag-plugin-android-okhttp

This module captures a breadcrumb for each network request made by OkHttp.

## High-level Overview

The module has a `compileOnly` dependency on OkHttp 4 and implements the `Plugin` and `EventListener`
interfaces. The user should install the plugin while initializing Bugsnag and creating an
`OkHttpClient`, and OkHttp's callbacks will then be used to automatically capture breadcrumbs
for each request.
