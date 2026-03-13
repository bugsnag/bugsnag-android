# bugsnag-plugin-android-okhttp

This module instruments OkHttp network requests, capturing them as breadcrumbs and/or errors
in Bugsnag.

## High-level Overview

`BugsnagOkHttp` is the main entry point. It provides a builder-style API to configure which HTTP
status codes should be reported as errors, request/response body capture limits, breadcrumb logging,
and request/response callbacks. Call `createInterceptor()` to produce an OkHttp `Interceptor` and
add it to your `OkHttpClient` via `addInterceptor`.

`OkHttpDelivery` is an OkHttp-backed `Delivery` implementation that can be used to deliver Bugsnag
event and session payloads using your own `OkHttpClient`.
