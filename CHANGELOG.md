# Changelog

## TBD

### Enhancements

* Added `Session.apiKey` so that it can be changed in an `OnSessionCallback`
  [#1855](https://github.com/bugsnag/bugsnag-android/pull/1855)

### Bug fixes

* Prevent rare app crash while migrating old `SharedPreferences` data from older versions of `bugsnag-android`
  [#1860](https://github.com/bugsnag/bugsnag-android/pull/1860)

* Prevent free memory calculation from potentially crashing the app when `ActivityManager` cannot be reached.
  [#1861](https://github.com/bugsnag/bugsnag-android/pull/1861)

## 5.30.0 (2023-05-11)

### Enhancements

* Optimise SessionTracer to reduce the overhead in calculating the current context Activity.
  [#1838](https://github.com/bugsnag/bugsnag-android/pull/1838)

* ANR or NDK detection warnings can be suppressed (using `enabledErrorTypes`) when plugin is excluded.
  [#1832](https://github.com/bugsnag/bugsnag-android/pull/1832)

## 5.29.0 (2023-03-23)

### Enhancements

* Added numeric range annotations to Configuration
  [#1808](https://github.com/bugsnag/bugsnag-android/pull/1808)
* Small improvements to the root detection overhead
  [#1815](https://github.com/bugsnag/bugsnag-android/pull/1815)

### Bug fixes

* Activity breadcrumbs now report the correct "previous" state
  [#1818](https://github.com/bugsnag/bugsnag-android/pull/1818)

## 5.28.4 (2023-02-08)

### Bug fixes

* Fixed a rare race-condition in libunwindstack where reading memory-maps could buffer-overrun
  [#1798](https://github.com/bugsnag/bugsnag-android/pull/1798)
* Fixed an extremely rare NDK race-condition where feature flags in native crashes would be corrupted
  [#1801](https://github.com/bugsnag/bugsnag-android/pull/1801)

## 5.28.3 (2022-11-16)

### Bug fixes

* Fixed a very rare race-condition in refreshSymbolTable that could lead to empty native stack traces being reported
  [#1781](https://github.com/bugsnag/bugsnag-android/pull/1781)

## 5.28.2 (2022-11-08)

### Bug fixes

* Fixed a bug that could sometimes cause native crashes when adding or clearing feature flags
  [#1777](https://github.com/bugsnag/bugsnag-android/pull/1777)
* Nested metadata is now correctly subject to the `Configuration.setMaxStringValueLength` setting
  [#1778](https://github.com/bugsnag/bugsnag-android/pull/1778)

## 5.28.1 (2022-10-19)

### Bug fixes

* Fixed rare thread-starvation issue where some internal failures could lead to deadlocks. This was most noticeable
  when attempting to call Bugsnag.start on an architecture (ABI) that was not packaged in the APK, and lead to an
  ANR instead of an error report.
  [#1768](https://github.com/bugsnag/bugsnag-android/pull/1768)

## 5.28.0 (2022-10-13)

### Enhancements

* Bugsnag now supports up to 500 breadcrumbs, with a default max of 100. Note that breadcrumbs will be trimmed
  (oldest first) if the payload exceeds 1MB.
  [#1751](https://github.com/bugsnag/bugsnag-android/pull/1751)

### Bug fixes

* Fixed very rare crashes when attempting to unwind NDK stacks over protected memory pages
  [#1761](https://github.com/bugsnag/bugsnag-android/pull/1761)

## 5.27.0 (2022-10-06)

### Enhancements

* Setting `Configuration.attemptDeliveryOnCrash` will cause Bugsnag to attempt error delivery during some crashes.
  Use of this feature is discouraged, see the method JavaDoc for more information.
  [#1749](https://github.com/bugsnag/bugsnag-android/pull/1749)

## 5.26.0 (2022-08-18)

### Enhancements

* Introduced `bugsnag_refresh_symbol_table` and `BugsnagNDK.refreshSymbolTable` to allow NDK apps to force a refresh of cached
  debug information used during a native crash. This new API is only applicable if you are using `dlopen` or `System.loadLibrary`
  after startup, and experiencing native crashes with missing symbols.
  [#1731](https://github.com/bugsnag/bugsnag-android/pull/1731)

### Bug fixes

* Non-List Collections are now correctly handled as OPAQUE values for NDK metadata
  [#1728](https://github.com/bugsnag/bugsnag-android/pull/1728)

## 5.25.0 (2022-07-19)

### Enhancements

* Feature flags can now be accessed in the onSend and onError callbacks
  [#1720](https://github.com/bugsnag/bugsnag-android/pull/1720)
* Feature flags are now kept in and trimmed in order of insertion or modification rather than in alphabetical order
  [#1718](https://github.com/bugsnag/bugsnag-android/pull/1718)

## 5.24.0 (2022-06-30)

### Enhancements

* Complex metadata (nested structures such as maps & lists) added in Java/Kotlin is now fully preserved in NDK errors
  [#1715](https://github.com/bugsnag/bugsnag-android/pull/1715)
* Configuration.discardClasses now applies to NDK errors
  [#1710](https://github.com/bugsnag/bugsnag-android/pull/1710)

## 5.23.1 (2022-06-23)

### Bug fixes

* Report the correct filename for native-libs that are loaded from within apk files
  [#1705](https://github.com/bugsnag/bugsnag-android/pull/1705)

## 5.23.0 (2022-06-20)

### Enhancements

* Added configuration option to control whether internal errors are sent to Bugsnag
  [#1701](https://github.com/bugsnag/bugsnag-android/pull/1701)

### Bug fixes

* Fixed Bugsnag interactions with the Google ANR handler on newer versions of Android
  [#1699](https://github.com/bugsnag/bugsnag-android/pull/1699)
* Overwriting & clearing event metadata in the NDK plugin will no longer leave phantom values
  [#1700](https://github.com/bugsnag/bugsnag-android/pull/1700)

## 5.22.4 (2022-05-24)

### Bug fixes

* Reverted [#1680](https://github.com/bugsnag/bugsnag-android/pull/1680) to avoid deadlocks during startup
  [#1696](https://github.com/bugsnag/bugsnag-android/pull/1696)
* Improved `app.inForeground` reporting for NDK errors
  [#1690](https://github.com/bugsnag/bugsnag-android/pull/1690)

## 5.22.3 (2022-05-12)

### Bug fixes

* Fixed concurrency bug that could be triggered via the React Native plugin
  [#1679](https://github.com/bugsnag/bugsnag-android/pull/1679)
* Correctly report `device.locationStatus` on Android 12 onwards using `LocationManager.isLocationEnabled`
  [1683](https://github.com/bugsnag/bugsnag-android/pull/1683)
* Small performance improvements to `Bugnag.start`
  [#1680](https://github.com/bugsnag/bugsnag-android/pull/1680)

## 5.22.2 (2022-05-04)

### Bug fixes

* Fixed NDK stack-traces for libraries linked after `Bugsnag.start` was called
  [#1671](https://github.com/bugsnag/bugsnag-android/pull/1671)

## 5.22.1 (2022-04-28)

### Enhancements

* Max reported threads can now be configured using manifest meta-data "com.bugsnag.android. MAX_REPORTED_THREADS"
  [#1655](https://github.com/bugsnag/bugsnag-android/pull/1655)
* Small improvement to startup performance (Bugsnag.start)
  [#1648](https://github.com/bugsnag/bugsnag-android/pull/1648)

## 5.22.0 (2022-03-31)

### Enhancements

* Added `Bugsnag.isStarted()` to test whether the Bugsnag client is in the middle of initializing. This can be used to guard uses of the Bugsnag API that are either on separate threads early in the app's start-up and so not guaranteed to be executed after `Bugsnag.start` has completed, or where Bugsnag may not have been started at all due to some internal app logic.
 [slack-jallen](https://github.com/slack-jallen):[#1621](https://github.com/bugsnag/bugsnag-android/pull/1621)
 [#1640](https://github.com/bugsnag/bugsnag-android/pull/1640)
  
* Events and Sessions will be discarded if they cannot be uploaded and are older than 60 days or larger than 1MB
  [#1633](https://github.com/bugsnag/bugsnag-android/pull/1633)

### Bug fixes

* Fixed potentially [thread-unsafe access](https://github.com/bugsnag/bugsnag-android/issues/883) when invoking `Bugsnag` static methods across different threads whilst `Bugsnag.start` is still in-flight. It is now safe to call any `Bugsnag` static method once `Bugsnag.start` has _begun_ executing, as access to the client singleton is controlled by a lock, so the new `isStarted` method (see above) should only be required where it cannot be determined whether the call to `Bugsnag.start` has begun or you do not want to wait. [#1638](https://github.com/bugsnag/bugsnag-android/pull/1638)
* Calling `bugsnag_event_set_context` with NULL `context` correctly clears the event context again
  [#1637](https://github.com/bugsnag/bugsnag-android/pull/1637)

## 5.21.0 (2022-03-17)

### Enhancements

* Fix inconsistencies in stack trace quality for C/C++ events. Resolves a few
  cases where file and line number information was not resolving to the correct
  locations. This change may result in grouping changes to more correctly
  highlight the root cause of an event.
  [#1605](https://github.com/bugsnag/bugsnag-android/pull/1605)
  [#1606](https://github.com/bugsnag/bugsnag-android/pull/1606)

### Bug fixes

* Fixed an issue where an uncaught exception on the main thread could in rare cases trigger an ANR.
  [#1624](https://github.com/bugsnag/bugsnag-android/pull/1624)

## 5.20.0 (2022-03-10)

### Enhancements

* The number of threads reported can now be limited using `Configuration.setMaxReportedThreads` (defaulting to 200)
  [#1607](https://github.com/bugsnag/bugsnag-android/pull/1607)
  
* Improved the performance and stability of the NDK and ANR plugins by caching JNI references on start
  [#1596](https://github.com/bugsnag/bugsnag-android/pull/1596)
  [#1601](https://github.com/bugsnag/bugsnag-android/pull/1601)

## 5.19.2 (2022-01-31)

### Bug fixes

* Fixed an issue where feature-flags were not always sent if an OnSendCallback was configured
  [#1589](https://github.com/bugsnag/bugsnag-android/pull/1589)

* Fix a bug where api keys set in React Native callbacks were ignored
  [#1592](https://github.com/bugsnag/bugsnag-android/pull/1592)

## 5.19.1 (2022-01-21)

### Bug fixes

* Discarded unhandled exceptions are propagated to any previously registered handlers
  [#1584](https://github.com/bugsnag/bugsnag-android/pull/1584)
  
* Fix SIGABRT crashes caused by race conditions in the NDK layer
  [#1585](https://github.com/bugsnag/bugsnag-android/pull/1585)

## 5.19.0 (2022-01-12)

* New APIs to support forthcoming feature flag and experiment functionality. For more information, please see https://docs.bugsnag.com/product/features-experiments.

### Enhancements

* Explicitly define Kotlin api/language versions
  [#1564](https://github.com/bugsnag/bugsnag-android/pull/1564)

* Build project with Kotlin 1.4, maintain compat with Kotlin 1.3
  [#1565](https://github.com/bugsnag/bugsnag-android/pull/1565)

## 5.18.0 (2022-01-05)

### Enhancements

* Improve the memory use and performance overhead when handling the delivery response status codes
  [#1558](https://github.com/bugsnag/bugsnag-android/pull/1558)
* Harden ndk layer through use of const keyword
  [#1566](https://github.com/bugsnag/bugsnag-android/pull/1566)

### Bug fixes

* Delete persisted NDK events earlier in delivery process
  [#1562](https://github.com/bugsnag/bugsnag-android/pull/1562)

* Add null checks for strlen()
  [#1563](https://github.com/bugsnag/bugsnag-android/pull/1563)

* Catch IOException when logging response status code
  [#1567](https://github.com/bugsnag/bugsnag-android/pull/1567)

## 5.17.0 (2021-12-08)

### Enhancements

* Bump compileSdkVersion to apiLevel 31
  [#1536](https://github.com/bugsnag/bugsnag-android/pull/1536)

### Bug fixes

* Flush in-memory sessions first
  [#1538](https://github.com/bugsnag/bugsnag-android/pull/1538)

* Avoid unnecessary network connectivity change breadcrumb
  [#1540](https://github.com/bugsnag/bugsnag-android/pull/1540)
  [#1546](https://github.com/bugsnag/bugsnag-android/pull/1546)

* Clear native stacktrace memory in `bugsnag_notify_env` before attempting to unwind the stack
  [#1543](https://github.com/bugsnag/bugsnag-android/pull/1543)

## 5.16.0 (2021-11-29)

### Bug fixes

* Increase resilience of NDK stackframe method capture
  [#1484](https://github.com/bugsnag/bugsnag-android/pull/1484)

* `redactedKeys` now correctly apply to metadata on Event breadcrumbs
  [#1526](https://github.com/bugsnag/bugsnag-android/pull/1526)
  
* Improved the robustness of automatically logged `ERROR` breadcrumbs
  [#1531](https://github.com/bugsnag/bugsnag-android/pull/1531)
  
* Improve performance on the breadcrumb storage "hot path" by removing Date formatting
  [#1525](https://github.com/bugsnag/bugsnag-android/pull/1525)

## 5.15.0 (2021-11-04)

### Bug fixes

* Avoid reporting false-positive background ANRs with improved foreground detection
  [#1429](https://github.com/bugsnag/bugsnag-android/pull/1429)

* Prevent events being attached to phantom sessions when they are blocked by an `OnSessionCallback`
  [#1434](https://github.com/bugsnag/bugsnag-android/pull/1434)

* Plugins will correctly mirror metadata added using `addMetadata(String, Map)`
  [#1454](https://github.com/bugsnag/bugsnag-android/pull/1454)

## 5.14.0 (2021-09-29)

### Enhancements 

* Capture and report thread state (running, sleeping, etc.) for Android Runtime and Native threads
  [#1367](https://github.com/bugsnag/bugsnag-android/pull/1367)
  [#1390](https://github.com/bugsnag/bugsnag-android/pull/1390)

## 5.13.0 (2021-09-22)

* Capture breadcrumbs for OkHttp network requests
  [#1358](https://github.com/bugsnag/bugsnag-android/pull/1358)
  [#1361](https://github.com/bugsnag/bugsnag-android/pull/1361)
  [#1363](https://github.com/bugsnag/bugsnag-android/pull/1363)
  [#1379](https://github.com/bugsnag/bugsnag-android/pull/1379)

* Update project to build using Gradle/AGP 7
  [#1354](https://github.com/bugsnag/bugsnag-android/pull/1354)

* Increased default breadcrumb collection limit to 50
  [#1366](https://github.com/bugsnag/bugsnag-android/pull/1366)

* Support integer values in buildUuid
  [#1375](https://github.com/bugsnag/bugsnag-android/pull/1375)

* Use SystemClock.elapsedRealtime to track `app.durationInForeground`
  [#1378](https://github.com/bugsnag/bugsnag-android/pull/1378)

## 5.12.0 (2021-08-26)

* The `app.lowMemory` value always report the most recent `onTrimMemory`/`onLowMemory` status
  [#1342](https://github.com/bugsnag/bugsnag-android/pull/1342)
  
* Added the `app.memoryTrimLevel` metadata to report a description of the latest `onTrimMemory` status
  [#1344](https://github.com/bugsnag/bugsnag-android/pull/1344)

* Added `STATE` Breadcrumbs for `onTrimMemory` events
  [#1345](https://github.com/bugsnag/bugsnag-android/pull/1345)

* The `device.totalMemory` and `device.freeMemory` values report device-level memory, and `app.memoryUsage`, `app.totalMemory`, `app.app.freeMemory`, and `app.memoryLimit` report VM level memory status
  [#1346](https://github.com/bugsnag/bugsnag-android/pull/1346)

## 5.11.0 (2021-08-05)

### Enhancements

* Add Bugsnag listeners for StrictMode violation detection
  [#1331](https://github.com/bugsnag/bugsnag-android/pull/1331)

### Bug fixes

* Address pre-existing StrictMode violations
  [#1328](https://github.com/bugsnag/bugsnag-android/pull/1328)

## 5.10.1 (2021-07-15)

### Bug fixes

* Prefer `calloc()` to `malloc()` in NDK code
  [#1320](https://github.com/bugsnag/bugsnag-android/pull/1320)

* Ensure correct value always collected for activeScreen
  [#1322](https://github.com/bugsnag/bugsnag-android/pull/1322)

## 5.10.0 (2021-07-14)

### Enhancements

* Capture process name in Event payload
  [#1318](https://github.com/bugsnag/bugsnag-android/pull/1318)

### Bug fixes

* Avoid unnecessary BroadcastReceiver registration for monitoring device orientation
  [#1303](https://github.com/bugsnag/bugsnag-android/pull/1303)

* Register system callbacks on background thread
  [#1292](https://github.com/bugsnag/bugsnag-android/pull/1292)

* Fix rare NullPointerExceptions from ConnectivityManager
  [#1311](https://github.com/bugsnag/bugsnag-android/pull/1311)

* Respect manual setting of context
  [#1310](https://github.com/bugsnag/bugsnag-android/pull/1310)

* Handle interrupt when shutting down executors
  [#1315](https://github.com/bugsnag/bugsnag-android/pull/1315)

* React Native: allow serializing enabledBreadcrumbTypes as null
  [#1316](https://github.com/bugsnag/bugsnag-android/pull/1316)

## 5.9.5 (2021-06-25)

* Unity: Properly handle ANRs after multiple calls to autoNotify and autoDetectAnrs
  [#1265](https://github.com/bugsnag/bugsnag-android/pull/1265)

* Cache value of app.backgroundWorkRestricted
  [#1275](https://github.com/bugsnag/bugsnag-android/pull/1275)

* Optimize execution of callbacks
  [#1276](https://github.com/bugsnag/bugsnag-android/pull/1276)

* Optimize implementation of internal state change observers
  [#1274](https://github.com/bugsnag/bugsnag-android/pull/1274)

* Optimize metadata implementation by reducing type casts
  [#1277](https://github.com/bugsnag/bugsnag-android/pull/1277)

* Trim stacktraces to <200 frames before attempting to construct POJOs
  [#1281](https://github.com/bugsnag/bugsnag-android/pull/1281)

* Use direct field access when adding breadcrumbs and state updates
  [#1279](https://github.com/bugsnag/bugsnag-android/pull/1279)

* Avoid using regex to validate api key
  [#1282](https://github.com/bugsnag/bugsnag-android/pull/1282)

* Discard unwanted automatic data earlier where possible
  [#1280](https://github.com/bugsnag/bugsnag-android/pull/1280)

* Enable ANR handling on immediately if started from the main thread
  [#1283](https://github.com/bugsnag/bugsnag-android/pull/1283)

* Include `app.binaryArch` in all events
  [#1287](https://github.com/bugsnag/bugsnag-android/pull/1287)

* Cache results from PackageManager
  [#1288](https://github.com/bugsnag/bugsnag-android/pull/1288)

* Use ring buffer to store breadcrumbs
  [#1286](https://github.com/bugsnag/bugsnag-android/pull/1286)

* Avoid expensive set construction in Config constructor
  [#1289](https://github.com/bugsnag/bugsnag-android/pull/1289)

* Replace calls to String.format() with concatenation
  [#1293](https://github.com/bugsnag/bugsnag-android/pull/1293)

* Optimize capture of thread traces
  [#1300](https://github.com/bugsnag/bugsnag-android/pull/1300)

## 5.9.4 (2021-05-26)

* Unity: add methods for setting autoNotify and autoDetectAnrs
  [#1233](https://github.com/bugsnag/bugsnag-android/pull/1233)

* Including bugsnag.h in C++ code will no longer cause writable-strings warnings
  [1260](https://github.com/bugsnag/bugsnag-android/pull/1260)

* Small performance improvements to device and app state collection
  [1258](https://github.com/bugsnag/bugsnag-android/pull/1258)

* NDK: lowMemory attribute is now reported as expected
  [1262](https://github.com/bugsnag/bugsnag-android/pull/1262)

* Don't include loglog.so in ndk plugin builds performed on Linux
  [1263](https://github.com/bugsnag/bugsnag-android/pull/1263)

## 5.9.3 (2021-05-18)

* Avoid unnecessary collection of Thread stacktraces
  [1249](https://github.com/bugsnag/bugsnag-android/pull/1249)

* Prevent errors in rare cases where either ConnectivityManager or StorageManager is not available
  [1251](https://github.com/bugsnag/bugsnag-android/pull/1251)

* Change the Bugsnag-Internal-Error header to "bugsnag-android"
  [1252](https://github.com/bugsnag/bugsnag-android/pull/1252)

* Prevent resource exhaustion when Throwable cause chains are recursive
  [1255](https://github.com/bugsnag/bugsnag-android/pull/1255)

* Added Date support to ObjectJsonStreamer
  [1256](https://github.com/bugsnag/bugsnag-android/pull/1256)

## 5.9.2 (2021-05-12)

### Bug fixes

* Guard against exceptions with null stack traces
  [#1239](https://github.com/bugsnag/bugsnag-android/pull/1239)

* Fix bug that terminated the app when multiple ANRs occur
  [#1235](https://github.com/bugsnag/bugsnag-android/pull/1235)

* Prevent rare NPE in log message
  [#1238](https://github.com/bugsnag/bugsnag-android/pull/1238)

* Prevent rare NPE when capturing thread traces
  [#1237](https://github.com/bugsnag/bugsnag-android/pull/1237)

* Catch exceptions thrown by Context.registerReceiver to prevent rare crashes
  [#1240](https://github.com/bugsnag/bugsnag-android/pull/1240)

* Fix possible NegativeArraySizeException in crash report deserialization
  [#1245](https://github.com/bugsnag/bugsnag-android/pull/1245)

## 5.9.1 (2021-04-22)

### Bug fixes

* Add projectPackages field to error payloads
  [#1226](https://github.com/bugsnag/bugsnag-android/pull/1226)

* Fix deserialization bug in persisted NDK errors
  [#1220](https://github.com/bugsnag/bugsnag-android/pull/1220)

## 5.9.0 (2021-03-30)

### Enhancements

* Improve detection of rooted devices
  [#1194](https://github.com/bugsnag/bugsnag-android/pull/1194)
  [#1195](https://github.com/bugsnag/bugsnag-android/pull/1195)
  [#1198](https://github.com/bugsnag/bugsnag-android/pull/1198)
  [#1200](https://github.com/bugsnag/bugsnag-android/pull/1200)
  [#1201](https://github.com/bugsnag/bugsnag-android/pull/1201)

* Bump compileSdkVersion to apiLevel 30
  [#1202](https://github.com/bugsnag/bugsnag-android/pull/1202)

* Collect whether the system has restricted background work for the app
  [#1211](https://github.com/bugsnag/bugsnag-android/pull/1211)

## 5.8.0 (2021-03-22)

### Deprecations

* `Configuration#launchCrashThresholdMs` is deprecated in favour of `Configuration#launchDurationMillis`

### Enhancements

* Add public API for crash-on-launch detection
  [#1157](https://github.com/bugsnag/bugsnag-android/pull/1157)
  [#1159](https://github.com/bugsnag/bugsnag-android/pull/1159)
  [#1165](https://github.com/bugsnag/bugsnag-android/pull/1165)
  [#1164](https://github.com/bugsnag/bugsnag-android/pull/1164)
  [#1182](https://github.com/bugsnag/bugsnag-android/pull/1182)
  [#1184](https://github.com/bugsnag/bugsnag-android/pull/1184)
  [#1185](https://github.com/bugsnag/bugsnag-android/pull/1185)
  [#1186](https://github.com/bugsnag/bugsnag-android/pull/1186)
  [#1180](https://github.com/bugsnag/bugsnag-android/pull/1180)
  [#1188](https://github.com/bugsnag/bugsnag-android/pull/1188)
  [#1191](https://github.com/bugsnag/bugsnag-android/pull/1191)

## 5.7.1 (2021-03-03)

### Bug fixes

* Fix for bad pointer access crash in JNI deliverReportAtPath
  [#1169](https://github.com/bugsnag/bugsnag-android/pull/1169)

## 5.7.0 (2021-02-18)

### Enhancements

* Support native stack traces in the ANR plugin
   [#972](https://github.com/bugsnag/bugsnag-android/pull/972)

### Bug fixes

* Check additional JNI calls for pending exceptions and no-op
  [#1142](https://github.com/bugsnag/bugsnag-android/pull/1142)
* Move free() call to exit block
  [#1140](https://github.com/bugsnag/bugsnag-android/pull/1140)
* Replace strncpy() usage with safe function call
  [#1149](https://github.com/bugsnag/bugsnag-android/pull/1149)
* Prevent NPE when delivering internal error reports
  [#1150](https://github.com/bugsnag/bugsnag-android/pull/1150)
* Further robustify string copying and JNI exception checks
  [#1153](https://github.com/bugsnag/bugsnag-android/pull/1153)

## 5.6.2 (2021-02-15)

### Bug fixes

* Check additional JNI calls for pending exceptions and no-op
  [#1133](https://github.com/bugsnag/bugsnag-android/pull/1133)

* Fix rare crash when loading device ID
  [#1137](https://github.com/bugsnag/bugsnag-android/pull/1137)

## 5.6.1 (2021-02-15)

The packaging for this version was incorrect so it should not be used.

## 5.6.0 (2021-02-08)

### Enhancements

* Enable React Native promise rejection handling
   [#1006](https://github.com/bugsnag/bugsnag-android/pull/1006)
   [#1001](https://github.com/bugsnag/bugsnag-android/pull/1001)

### Bug fixes

* Check internal JNI calls for pending exceptions and no-op
  [#1088](https://github.com/bugsnag/bugsnag-android/pull/1088)
  [#1091](https://github.com/bugsnag/bugsnag-android/pull/1091)
  [#1092](https://github.com/bugsnag/bugsnag-android/pull/1092)
  [#1117](https://github.com/bugsnag/bugsnag-android/pull/1117)

* Add global metadata to ANR error reports
  [#1095](https://github.com/bugsnag/bugsnag-android/pull/1095)

## 5.5.2 (2021-01-27)

### Bug fixes

* Fix regression in 5.5.1 where ANR and NDK detection was not functional for apps using ProGuard/R8 or DexGuard
  [#1096](https://github.com/bugsnag/bugsnag-android/pull/1096)

## 5.5.1 (2021-01-21)

### Bug fixes

* Alter ANR SIGQUIT handler to stop interfering with Google's ANR reporting, and to avoid unsafe JNI calls from within a signal handler
  [#1078](https://github.com/bugsnag/bugsnag-android/pull/1078)

* Alter HTTP requests to stop using chunked transfer encoding
  [#1077](https://github.com/bugsnag/bugsnag-android/pull/1077)

* Allow null device IDs, preventing rare crash in Bugsnag initialization
  [#1083](https://github.com/bugsnag/bugsnag-android/pull/1083)

## 5.5.0 (2021-01-07)

### Enhancements

This release supports initializing Bugsnag in multi processes apps. If your app uses Bugsnag in multiple processes, you should initialize Bugsnag
with a unique `persistenceDirectory` value for each process. Please see [the docs](https://docs.bugsnag.com/platforms/android/faq/#does-bugsnag-support-multi-process-apps) for further information.

* Store user information in persistenceDirectory
  [#1017](https://github.com/bugsnag/bugsnag-android/pull/1017)

* Use consistent device ID for multi process apps
  [#1013](https://github.com/bugsnag/bugsnag-android/pull/1013)

* Create synchronized store for user information
  [#1010](https://github.com/bugsnag/bugsnag-android/pull/1010)

* Add persistenceDirectory config option for controlling event/session storage
  [#998](https://github.com/bugsnag/bugsnag-android/pull/998)

* Add configuration option to control maximum number of persisted events/sessions
  [#980](https://github.com/bugsnag/bugsnag-android/pull/980)

* Increase kotlin dependency version to 1.3.72
  [#1050](https://github.com/bugsnag/bugsnag-android/pull/1050)

## 5.4.0 (2020-12-14)

### Enhancements


* Make `event.unhandled` overridable for NDK errors
  [#1037](https://github.com/bugsnag/bugsnag-android/pull/1037)

* Make `event.unhandled` overridable for React Native errors
  [#1039](https://github.com/bugsnag/bugsnag-android/pull/1039)

* Make `event.unhandled` overridable for JVM errors
  [#1025](https://github.com/bugsnag/bugsnag-android/pull/1025)

### Bug fixes

* Prevent potential SHA-1 hash mismatch in Bugsnag-Integrity header for session requests
  [#1043](https://github.com/bugsnag/bugsnag-android/pull/1043)

## 5.3.1 (2020-12-09)

### Bug fixes

* Prevent potential SHA-1 hash mismatch in Bugsnag-Integrity header
  [#1028](https://github.com/bugsnag/bugsnag-android/pull/1028)

## 5.3.0 (2020-12-02)

* Add integrity header to verify Error and Session API payloads have not changed
  [#978](https://github.com/bugsnag/bugsnag-android/pull/978)

## 5.2.3 (2020-11-04)

### Bug fixes

* Flush persisted sessions on launch and on connectivity changes
  [#973](https://github.com/bugsnag/bugsnag-android/pull/973)

* Increase breadcrumb time precision to milliseconds
  [#954](https://github.com/bugsnag/bugsnag-android/pull/954)

* Default to allowing requests when checking connectivity
  [#970](https://github.com/bugsnag/bugsnag-android/pull/970)

* Support changing NDK Event's api key in OnErrorCallback
  [#964](https://github.com/bugsnag/bugsnag-android/pull/964)

## 5.2.2 (2020-10-19)

### Bug fixes

* Avoid crash when initializing bugsnag in attachBaseContext
  [#953](https://github.com/bugsnag/bugsnag-android/pull/953)

* Prevent ConcurrentModificationException when setting redactedKeys
  [#947](https://github.com/bugsnag/bugsnag-android/pull/947)

## 5.2.1 (2020-10-01)

### Bug fixes

* Support changing Event's api key in OnErrorCallback
  [#928](https://github.com/bugsnag/bugsnag-android/pull/928)

* Ensure device ID is set separately to the user ID
  [#939](https://github.com/bugsnag/bugsnag-android/pull/939)

* Improve stack traces and grouping for promise rejections on React Native < 0.63.2
  [#940](https://github.com/bugsnag/bugsnag-android/pull/940)

## 5.2.0 (2020-09-22)

### Bug fixes

* Prevent ConcurrentModificationException thrown from Metadata class
  [#935](https://github.com/bugsnag/bugsnag-android/pull/935)

* Prevent incorrect merge of nested maps in metadata
  [#936](https://github.com/bugsnag/bugsnag-android/pull/936)

#### React Native

* Improve stack traces and grouping for React Native promise rejections
  [#937](https://github.com/bugsnag/bugsnag-android/pull/937)

## 5.1.0 (2020-09-08)

### Enhancements

* Add accessor for breadcrumb list on Client
  [#924](https://github.com/bugsnag/bugsnag-android/pull/924)

* Test improvement: removed conditional operator test smell
  [#925](https://github.com/bugsnag/bugsnag-android/pull/925)

#### React Native

* Keep name of class for use in reflection
  [#927](https://github.com/bugsnag/bugsnag-android/pull/927)

## 5.0.2 (2020-08-17)

### Bug fixes

#### React Native

The following alterations have been made to support the React Native notifier:

* Split updateMetadata method into two separate add/clear methods
  [#918](https://github.com/bugsnag/bugsnag-android/pull/918)

* Refactor event deserialiser to ensure unhandled value is retained
  [#917](https://github.com/bugsnag/bugsnag-android/pull/917)

* Retain unhandled value when parsing a JS event
  [#914](https://github.com/bugsnag/bugsnag-android/pull/914)

* Prevent duplicate notifier dependencies being added
  [#911](https://github.com/bugsnag/bugsnag-android/pull/911)

* Attempt delivery of promise rejections immediately
  [#912](https://github.com/bugsnag/bugsnag-android/pull/912)

* Always set config.redactedKeys on Event
  [#913](https://github.com/bugsnag/bugsnag-android/pull/913)

## 5.0.1 (2020-07-23)

### Bug fixes

* Add null check when loading data from manifest
  [#878](https://github.com/bugsnag/bugsnag-android/pull/878)

* Fix swapped device freeMemory and freeDisk values [#903](https://github.com/bugsnag/bugsnag-android/issues/903)

* Avoid attempting to flush unhandled error reports in same session
  [#902](https://github.com/bugsnag/bugsnag-android/pull/902)

#### React Native

The following alterations have been made to support the React Native notifier:

* Prevent duplicate notifier dependencies being added
  [#911](https://github.com/bugsnag/bugsnag-android/pull/911)

* Respect pre-populated fields on `Event` when notifying
  [#906](https://github.com/bugsnag/bugsnag-android/pull/906)

* Create observer before registering plugin
  [#874](https://github.com/bugsnag/bugsnag-android/pull/874)

* `app.type` event serialisation differs from config
  [#909](https://github.com/bugsnag/bugsnag-android/pull/909)

* Pass device and app metadata to react native
  [#910](https://github.com/bugsnag/bugsnag-android/pull/910)

## 5.0.0 (2020-04-22)

__This version contains many breaking changes__. It is part of an effort to unify our notifier
libraries across platforms, making the user interface more consistent, and implementations better
 on multi-layered environments where multiple Bugsnag libraries need to work together
 (such as React Native).

Please see the [upgrade guide](UPGRADING.md) for details of all the changes and instructions on
how to upgrade.

### Bug fixes

* Remove unnecessary uses-library android.test.runner from AndroidManifest
  [#783](https://github.com/bugsnag/bugsnag-android/pull/783)

## 4.22.3 (2020-01-22)

* Allow disabling previous signal handler invocation for Unity ANRs
  [#743](https://github.com/bugsnag/bugsnag-android/pull/743)

* Avoid polling when detecting ANRs by invoking JNI from SIGQUIT handler
  [#741](https://github.com/bugsnag/bugsnag-android/pull/741)

## 4.22.2 (2020-01-06)

### Bug fixes

* Fix: address CVE-2019-10101 by increasing Kotlin version to 1.3.61
  [#739](https://github.com/bugsnag/bugsnag-android/pull/739)
* Catch throwables when invoking methods on system services
  [#623](https://github.com/bugsnag/bugsnag-android/pull/623)
* Fix possible crash when recording reports and breadcrumbs containing values
  using different text encodings or UTF-8 control characters, followed by a
  C/C++ crash.
  [#584](https://github.com/bugsnag/bugsnag-android/pull/584)
* Fix crash when calling `NativeInterface.clearTab()` (from an integration
  library)
  [#582](https://github.com/bugsnag/bugsnag-android/pull/582)
* Fix abort() in native code when storing breadcrumbs with null values in
  metadata
  [#510](https://github.com/bugsnag/bugsnag-android/pull/510)
* Fix potential segfaults when adding breadcrumb with NDK
  [#546](https://github.com/bugsnag/bugsnag-android/pull/546)
* Convert metadata to map when notifying the NDK observer
  [#513](https://github.com/bugsnag/bugsnag-android/pull/513)
* Prevent overwrite of signal mask when installing ANR handler
  [#520](https://github.com/bugsnag/bugsnag-android/pull/520)
* Fix possible null pointer exception when creating a breadcrumb without
  metadata
  [#585](https://github.com/bugsnag/bugsnag-android/pull/585)

## 4.21.1 (2019-10-15)

* Fix a packaging issue on Maven Central in v4.21.0

## 4.21.0 (2019-10-14)

* Collect additional data in internal error reports
  [#612](https://github.com/bugsnag/bugsnag-android/pull/612)

* Allow overriding the versionCode via Configuration
  [#610](https://github.com/bugsnag/bugsnag-android/pull/610)

### Bug fixes

* Delete cached error reports if an Exception is thrown during disk IO, preventing delivery of empty/partial reports on the next app launch.
  [#609](https://github.com/bugsnag/bugsnag-android/pull/609)

* Prevent internal error reporting of FileNotFoundException during serialization
  [#605](https://github.com/bugsnag/bugsnag-android/pull/605)

## 4.20.0 (2019-09-25)

* Record StorageManager cache behaviour in internal error reports
  [#588](https://github.com/bugsnag/bugsnag-android/pull/588)

* Delete empty files left in cache directory, preventing an erroneous source of minimal error reports
  [#591](https://github.com/bugsnag/bugsnag-android/pull/591)

* Report internal errors when serialization fails
  [#581](https://github.com/bugsnag/bugsnag-android/pull/581)

* Buffer IO when reading from cached error files, improving SDK performance
  [#573](https://github.com/bugsnag/bugsnag-android/pull/573)

* Prevent internal error reporting of FileNotFoundException during Delivery
  [#594](https://github.com/bugsnag/bugsnag-android/pull/594)

### Bug fixes

* flushOnLaunch() does not cancel previous requests if they timeout, leading to potential duplicate reports
  [#593](https://github.com/bugsnag/bugsnag-android/pull/593)

* Alter value collected for device.freeDisk to collect usable space in internal storage,
 rather than total space in internal/external storage
  [#589](https://github.com/bugsnag/bugsnag-android/pull/589)

* Buffer io when reading from cached error file
  [#573](https://github.com/bugsnag/bugsnag-android/pull/573)

* Fix possible crash when recording reports and breadcrumbs containing values
  using different text encodings or UTF-8 control characters, followed by a
  C/C++ crash.
  [#584](https://github.com/bugsnag/bugsnag-android/pull/584)

## 4.19.1 (2019-09-03)

### Bug fixes

* Fix deserialization of custom stackframe fields in cached error reports
  [#576](https://github.com/bugsnag/bugsnag-android/pull/576)

* Fix potential null pointer exception if `setMetaData` is called with a null
  value

## 4.19.0 (2019-08-27)

* Report internal SDK errors to bugsnag
  [#570](https://github.com/bugsnag/bugsnag-android/pull/570)

## 4.18.0 (2019-08-15)

* Migrate dependencies to androidx
  [#554](https://github.com/bugsnag/bugsnag-android/pull/554)

* Improve ANR error message information
  [#553](https://github.com/bugsnag/bugsnag-android/pull/553)

## 4.18.0-beta01 (2019-08-09)

* Improve ANR error message information
  [#553](https://github.com/bugsnag/bugsnag-android/pull/553)

## 4.17.2 (2019-08-01)

### Bug fixes
* Fix potential segfaults when adding breadcrumb with NDK
  [#546](https://github.com/bugsnag/bugsnag-android/pull/546)

## 4.17.1 (2019-07-24)

### Bug fixes
* Fix NPE causing crash when reporting a minimal error
  [#534](https://github.com/bugsnag/bugsnag-android/pull/534)

## 4.17.0 (2019-07-17)

This release modularizes `bugsnag-android` into 3 separate artifacts: for JVM (Core), NDK, and ANR error
detection. You should use v4.5.2 or above of the [bugsnag android gradle plugin](https://github.com/bugsnag/bugsnag-android-gradle-plugin/releases/tag/v4.5.2) in order to use this version, then add a compile-time dependency on either `bugsnag-android` or `bugsnag-android-ndk` as before.

If you do not wish to use the NDK/ANR artifacts, it is possible to exclude these via gradle. You
should also set `detectNdkCrashes` and `detectAnrs` to false on the `Configuration` object supplied
during `Bugsnag.init()`.

```groovy
implementation("com.bugsnag:bugsnag-android:$version") {
    exclude group: "com.bugsnag", module: "bugsnag-plugin-android-anr"
    exclude group: "com.bugsnag", module: "bugsnag-plugin-android-ndk"
}
```

* Modularise bugsnag-android into Core, NDK, and ANR artifacts
  [#522](https://github.com/bugsnag/bugsnag-android/pull/522)

## 4.16.1 (2019-07-10)

### Bug fixes
* Prevent overwrite of signal mask when installing ANR handler
  [#520](https://github.com/bugsnag/bugsnag-android/pull/520)

## 4.16.0 (2019-07-09)

This release adds a compile-time dependency on the Kotlin standard library. This should not affect
the use of any API supplied by bugsnag-android.

* Use NetworkCallback to monitor connectivity changes on newer API levels
[#501](https://github.com/bugsnag/bugsnag-android/pull/501)
* Send minimal error report if cached file is corrupted/empty
[#500](https://github.com/bugsnag/bugsnag-android/pull/500)
* Remove deprecated interfaces from API
[#514](https://github.com/bugsnag/bugsnag-android/pull/514)

### Bug fixes

* Fix abort() in native code when storing breadcrumbs with null values in
  metadata
  [#510](https://github.com/bugsnag/bugsnag-android/pull/510)
* Convert metadata to map when notifying the NDK observer
  [#513](https://github.com/bugsnag/bugsnag-android/pull/513)

## 4.15.0 (2019-06-10)

`bugsnag-android` now supports detecting and reporting C/C++ crashes without a separate library (previously `bugsnag-android-ndk` was required for native error reporting).

`bugsnag-android` and `bugsnag-android-ndk` have essentially been merged. The only difference is that in `bugsnag-android-ndk`, NDK crash detection is enabled by default. To enable it in `bugsnag-android`, the configuration option `detectNdkCrashes` should be set to true.

After the next major release `bugsnag-android-ndk` will no longer be published, so it is recommended that you migrate to the `bugsnag-android` artefact.

### Enhancements

* Improve ANR detection by using a signal handler to detect `SIGQUIT`
  events, removing dependence on "in foreground" calculations. This change
  should remove false positives. This change deprecates the configuration
  options `setAnrThresholdMs`/`getAnrThresholdMs` as they now have no effect and
  the underlying OS ANR threshold is used in all cases.
  [#490](https://github.com/bugsnag/bugsnag-android/pull/490)

* Add `detectNdkCrashes` configuration option to toggle whether C/C++ crashes
  are detected
  [#491](https://github.com/bugsnag/bugsnag-android/pull/491)

* Reduce AAR size [#492](https://github.com/bugsnag/bugsnag-android/pull/492)

* Make handledState.isUnhandled() publicly readable [#496](https://github.com/bugsnag/bugsnag-android/pull/496)

## 4.14.2 (2019-05-21)

### Enhancements

* Disable ANR detection by default [#484](https://github.com/bugsnag/bugsnag-android/pull/484)

## 4.14.1 (2019-05-17)

### Bug fixes

* Remove `RINGER_MODE_CHANGED` action from broadcast receiver, which fixes a `SecurityException` thrown in Instant Apps
[#481](https://github.com/bugsnag/bugsnag-android/pull/481)

* Reduce sensitivity of Foreground Detection, reducing potential ANR false positives
[#482](https://github.com/bugsnag/bugsnag-android/pull/482)

## 4.14.0 (2019-05-07)

### Enhancements

* Alter In foreground calculation
[#466](https://github.com/bugsnag/bugsnag-android/pull/466)

* Migrate version information to device.runtimeVersions
[#472](https://github.com/bugsnag/bugsnag-android/pull/472)

* Add internal api for mutating session payload before sending
[#472](https://github.com/bugsnag/bugsnag-android/pull/474)

* Resolve pre-existing Android Inspection violations
[#468](https://github.com/bugsnag/bugsnag-android/pull/468)

### Bug fixes

* [NDK] Fix possible null pointer dereference
* [NDK] Fix possible memory leak if bugsnag-android-ndk fails to successfully
  parse a cached crash report
* [NDK] Fix possible memory leak when using `bugsnag_leave_breadcrumb()` or
  `bugsnag_notify()`

## 4.13.0 (2019-04-03)

### Enhancements

* Add ANR detection to bugsnag-android
[#442](https://github.com/bugsnag/bugsnag-android/pull/442)

* Add unhandled_events field to native payload
[#445](https://github.com/bugsnag/bugsnag-android/pull/445)

### Bug fixes

* Ensure boolean object from map serialised as boolean primitive in JNI
[#452](https://github.com/bugsnag/bugsnag-android/pull/452)

* Prevent NPE occurring when calling resumeSession()
[#444](https://github.com/bugsnag/bugsnag-android/pull/444)

* Read projectPackages array when serialising error reports
[#451](https://github.com/bugsnag/bugsnag-android/pull/451)

## 4.12.0 (2019-02-27)

### Enhancements

* Add stopSession() and resumeSession() to Client
[#429](https://github.com/bugsnag/bugsnag-android/pull/429)

### Bug fixes

* Prevent overwriting config.projectPackages if already set
  [#428](https://github.com/bugsnag/bugsnag-android/pull/428)

* Fix incorrect session handledCount when notifying in quick succession
  [#434](https://github.com/bugsnag/bugsnag-android/pull/434)

## 4.11.0 (2019-01-22)

### Enhancements

* [NDK] Improve support for C++ exceptions, adding the exception class name
  and description to reports and improving the stacktrace quality
  [#412](https://github.com/bugsnag/bugsnag-android/pull/412)

* Update vendored GSON dependency to latest available version
[#415](https://github.com/bugsnag/bugsnag-android/pull/415)

### Bug fixes

* Fix cached error deserialisation where the Throwable has a cause
  [#418](https://github.com/bugsnag/bugsnag-android/pull/418)

* Refactor error report deserialisation
  [#419](https://github.com/bugsnag/bugsnag-android/pull/419)

* Fix unlikely initialization failure if a device orientation event listener
  cannot be enabled

* Cache result of device root check
  [#411](https://github.com/bugsnag/bugsnag-android/pull/411)

* Prevent unnecessary free disk calculations on initialisation
 [#409](https://github.com/bugsnag/bugsnag-android/pull/409)

## 4.10.0 (2019-01-07)

* Improve kotlin support by allowing property access
 [#393](https://github.com/bugsnag/bugsnag-android/pull/393)

* Added additional nullability annotations to public API
 [#395](https://github.com/bugsnag/bugsnag-android/pull/395)

* Migrate metaData.device.cpuAbi to device.cpuAbi in JSON payload
 [#404](https://github.com/bugsnag/bugsnag-android/pull/404)

### Bug fixes

* Add binary architecture of application to payload
 [#389](https://github.com/bugsnag/bugsnag-android/pull/389)

* Prevent errors from leaving a self-referencing breadcrumb
 [#391](https://github.com/bugsnag/bugsnag-android/pull/391)

* Fix calculation of durationInForeground when autoCaptureSessions is false
 [#394](https://github.com/bugsnag/bugsnag-android/pull/394)

* Prevent Bugsnag.init from instantiating more than one client
 [#403](https://github.com/bugsnag/bugsnag-android/pull/403)

* Make config.metadata publicly accessible
 [#406](https://github.com/bugsnag/bugsnag-android/pull/406)

## 4.9.3 (2018-11-29)

### Bug fixes

* Handle null values in MetaData.mergeMaps, preventing potential NPE
 [#386](https://github.com/bugsnag/bugsnag-android/pull/386)


## 4.9.2 (2018-11-07)

### Bug fixes

* [NDK] Fix regression in 4.9.0 which truncated stacktraces on 64-bit devices to
  a single frame
  [#383](https://github.com/bugsnag/bugsnag-android/pull/383)

## 4.9.1 (2018-11-01)

### Bug fixes

* Allow setting context to null from callbacks
  [#381](https://github.com/bugsnag/bugsnag-android/pull/381)

## 4.9.0 (2018-10-29)

### Enhancements

* Add a callback to allow modifying reports immediately prior to delivery,
  including fatal crashes from native C/C++ code. For more information, see the
  [callback reference](https://docs.bugsnag.com/platforms/android/sdk/customizing-error-reports).
  [#379](https://github.com/bugsnag/bugsnag-android/pull/379)

### Bug fixes

* [NDK] Improve stack trace quality for signals raised on ARM32 devices
  [#378](https://github.com/bugsnag/bugsnag-android/pull/378)

## 4.8.2 (2018-10-01)

### Bug fixes

* Add ThreadSafe annotation to com.bugsnag.android, remove infer dependency
  [#370](https://github.com/bugsnag/bugsnag-android/pull/370)
  [#366](https://github.com/bugsnag/bugsnag-android/issues/366)

## 4.8.1 (2018-09-27)

* [NDK] Fix a packaging issue on Maven Central in v4.8.0

## 4.8.0 (2018-09-27)

This release includes new versions of both bugsnag-android and
bugsnag-android-ndk, which will be released at the same time and with the same
version number going forward.

The NDK library has been rebuilt from the ground up and will use the same
version number as bugsnag-android.

### Enhancements

* [NDK] Improve stack trace quality for all native crashes. There should be
  significantly more crash-time information, across the board but especially on
  newer API levels.
* [NDK] Reduce memory usage
* [NDK] Add support for session information
* [NDK] Add support for unhandled report tracking

### Bug Fixes

* [NDK] Fix possible crash when leaving breadcrumbs from multiple threads
  [#6](https://github.com/bugsnag/bugsnag-android-ndk/issues/6)
  [#10](https://github.com/bugsnag/bugsnag-android-ndk/issues/10)


## 4.7.0 (2018-09-26)

* Capture trace of error reporting thread and identify with boolean flag [#355](https://github.com/bugsnag/bugsnag-android/pull/355)

## 4.6.1 (2018-08-21)

### Bug fixes

* Set maxBreadcrumbs via Configuration rather than Client [#359](https://github.com/bugsnag/bugsnag-android/pull/359)
* Catch Exception within DefaultDelivery class [#361](https://github.com/bugsnag/bugsnag-android/pull/361)
* Add Null check when accessing system service [#367](https://github.com/bugsnag/bugsnag-android/pull/367)

## 4.6.0 (2018-08-02)

* Android P compatibility fixes - ensure available information on StrictMode violations is collected [#350](https://github.com/bugsnag/bugsnag-android/pull/350)

* Disable BuildConfig generation [#343](https://github.com/bugsnag/bugsnag-android/pull/343)

* Add consumer proguard rules for automatic ProGuard configuration without the Bugsnag gradle plugin [#345](https://github.com/bugsnag/bugsnag-android/pull/345)

* Internal refactor of app/device data serialisation

## 4.5.0 (2018-06-18)

This release alters the behaviour of the notifier to track sessions automatically.
A session will be automatically captured on each app launch and sent to [https://sessions.bugsnag.com](https://sessions.bugsnag.com). If you
use Bugsnag On-Premise, it is now also recommended that you set your notify and session endpoints
via `config.setEndpoints(String notify, String sessions)`.

* Enable automatic session tracking by default [#314](https://github.com/bugsnag/bugsnag-android/pull/314)

### Bug fixes

* Trim long stacktraces to max limit of 200 [#324](https://github.com/bugsnag/bugsnag-android/pull/324)

## 4.4.1 (2018-05-30)

### Bug fixes

* Refine automatically collected breadcrumbs to a commonly useful set by default
[#321](https://github.com/bugsnag/bugsnag-android/pull/321)

* Ensure that unhandled error reports are always sent immediately on launch for Android P and in situations with no connectivity.
[#319](https://github.com/bugsnag/bugsnag-android/pull/319)

## 4.4.0 (2018-05-17)

### Features

Deprecation notice:

SessionTrackingApiClient and ErrorApiClient are now deprecated in favour of the Delivery interface.
If you configure a custom HTTP client with Bugsnag, it is recommended that you migrate over to this new API.
Further information is available [in the configuration option reference.](https://docs.bugsnag.com/platforms/android/sdk/configuration-options/#setdelivery)
and [class documentation for `Delivery`](https://bugsnag.github.io/bugsnag-android/com/bugsnag/android/Delivery.html)

* Expose Delivery API interface for configuring custom HTTP clients
[#299](https://github.com/bugsnag/bugsnag-android/pull/299)

### Enhancements

* Use buffered streams for IO (perf improvement)
[#307](https://github.com/bugsnag/bugsnag-android/pull/307)

## 4.3.4 (2018-05-02)

### Bug fixes

* Avoid adding extra comma separator in JSON if File input is empty or null
[#284](https://github.com/bugsnag/bugsnag-android/pull/284)

* Thread safety fixes to JSON file serialisation
[#295](https://github.com/bugsnag/bugsnag-android/pull/295)

* Prevent potential automatic activity lifecycle breadcrumb crash
[#300](https://github.com/bugsnag/bugsnag-android/pull/300)

* Fix serialisation issue with leading to incorrect dashboard display of breadcrumbs
[#306](https://github.com/bugsnag/bugsnag-android/pull/306)

## 4.3.3 (2018-04-04)

### Bug fixes

* Prevent duplicate reports being delivered in low connectivity situations
  [#270](https://github.com/bugsnag/bugsnag-android/pull/270)
* Fix possible NPE when reading default metadata filters
  [#263](https://github.com/bugsnag/bugsnag-android/pull/263)

## 4.3.2 (2018-03-09)

### Bug fixes

* Prevent ConcurrentModificationException in Before notify/breadcrumb callbacks [#266](https://github.com/bugsnag/bugsnag-android/pull/266)
* Ensure that exception message is never null [#256](https://github.com/bugsnag/bugsnag-android/pull/256)
* Add payload version to JSON body [#244](https://github.com/bugsnag/bugsnag-android/pull/244)
* Update context tracking to use lifecycle callbacks rather than ActivityManager [#238](https://github.com/bugsnag/bugsnag-android/pull/238)

### Enhancements

* Detect whether running on emulator [#245](https://github.com/bugsnag/bugsnag-android/pull/245)
* Add a callback for filtering breadcrumbs [#237](https://github.com/bugsnag/bugsnag-android/pull/237)


## 4.3.1 (2018-01-26)

### Bug fixes

* Fix possible ANR when enabling session tracking via
  `Bugsnag.setAutoCaptureSessions()` and connecting to latent networks.
  [#231](https://github.com/bugsnag/bugsnag-android/pull/231)

* Fix invalid payloads being sent when processing multiple Bugsnag events in the
  same millisecond
  [#235](https://github.com/bugsnag/bugsnag-android/pull/235)

* Re-add API key to error report HTTP request body to preserve backwards
  compatibility with older versions of the error reporting API
  [#228](https://github.com/bugsnag/bugsnag-android/pull/228)

## 4.3.0 (2018-01-18)

- Move capture of thread stacktraces to start of notify process
- Add configuration option to disable automatic breadcrumb capture
- Update Gradle Wrapper
- Parse manifest meta-data for Session Auto-Capture boolean flag

## 4.2.2 (2018-01-09)

### Bug fixes

- Fix possible crash during session tracking initialization
  [#220](https://github.com/bugsnag/bugsnag-android/pull/220)
  [James Smith](https://github.com/loopj)

## 4.2.1 (2018-01-09)
- Misc Session Tracking fixes and enhancements

## 4.2.0 (2018-01-05)
- Adds support for tracking sessions and overall crash rate by setting `config.setAutoCaptureSessions` to `true`.
In addition, sessions can be indicated manually using `Bugsnag.startSession` [#217](https://github.com/bugsnag/bugsnag-android/pull/217)

## 4.1.5 (2017-12-14)
- Automatically capture breadcrumbs for new API 26 Intents
- Increase max breadcrumb limit
- Remove known noisy breadcrumbs from automatic capture

## 4.1.4 (2017-11-23)
- Enqueue activity lifecycle events when initialisation not complete to prevent NPE
- Add example of using Bugsnag within a library module

## 4.1.3 (2017-11-07)
- Compile annotations dependency as api rather than implementation
- Support handled state case for React Native

## 4.1.2 (2017-11-02)
- Allow setting device ID to null

## 4.1.1 (2017-10-12)
- Performance improvements to reduce execution time of `Bugsnag.init`

## 4.1.0 (2017-10-02)
- The SDK now automatically tracks whether an error is handled or unhandled.
- Fix for NPE in MetaData callback [Boris](https://github.com/borhub)
- Updated example app.
- Setting the maxSize of breadcrumbs now removes any surplus breadcrumbs.
- Crash reports on application startup are automatically sent synchronously on the next launch (configurable via `setLaunchCrashThresholdMs`).

## 4.0.0 (2017-08-15)

This is a major release which adds a number of new features to the library. The minimum SDK version supported by Bugsnag is now API 14.

### Breaking Changes

Identifying devices and users (unless overridden) is now captured and persisted
through a per-install generated UUID, replacing use of
`Settings.Secure.ANDROID_ID`. As a result, existing errors may see doubled user
counts if they continue to happen. If this is an issue, see the [upgrade
guide](UPGRADING.md#upgrade-from-3x-to-4x)
for more information and a workaround.

### Enhancements
- Support loading different API keys for different product flavors, through manifest placeholders

- Support custom HTTP Error Reporting clients, by allowing custom implementations of `ErrorReportApiClient`

- Add nullability annotations throughout application

- Any exceptions caused by StrictMode will automatically add the policy violation type to the Error Report's metadata

- All tests have been updated to use JUnit 4 rather than `AndroidTestCase`

- Javadoc for the public API is now hosted on Github Pages

- Breadcrumbs are automatically logged for each callback in the Activity Lifecycle, for all activities

- Breadcrumbs are automatically logged for most System Intents (e.g. `android.intent.action.CONFIGURATION_CHANGED`)

- Optimize how reports are sent relative to network connectivity to improve battery life

- Added documentation on how Breadcrumbs can be setup to track the Fragment Lifecycle

- Added Kotlin example app and documentation

- Disable logs by default on release builds

### Bug Fixes
- Handle `RejectionExecutionException` by writing unqueued Error reports to disk
[Damian Wieczorek](https://github.com/damianw)

- Handle IllegalStateException caused by `CharsetEncoder` on Android 6.0
[Ben Lee](https://github.com/Bencodes)

- Each implementation of `beforeNotify()` is now only called once, in the order in which it was added
[jermainedilao](https://github.com/jermainedilao)

- By default, the User ID is now a per-install UUID, whereas previously `Settings.Secure.ANDROID_ID` was used
[Martin Georgiev](https://github.com/georgiev-martin)

- Update Gradle dependencies
[Frieder Bluemle](https://github.com/friederbluemle)


## 3.9.0 (2017-05-08)

### Enhancements

* Improve performance by using bounded `ThreadPoolExecutor` for asynchronous
  `notify()` calls
  [Felipe Lima](https://github.com/felipecsl)
  [#145](https://github.com/bugsnag/bugsnag-android/pull/145)

* Detect systemless root
  [Matthias Urhahn](https://github.com/d4rken)
  [#142](https://github.com/bugsnag/bugsnag-android/pull/142)

## 3.8.0 (2017-01-27)

## Enhancements

* Add support for interfacing with native code

## 3.7.2 (2017-01-12)

* Cache unhandled exception reports prior to sending, send non-blocking
  [Delisa Mason](https://github.com/kattrali)
  [#139](https://github.com/bugsnag/bugsnag-android/pull/139)

## 3.7.1 (2016-12-21)

### Bug fixes

* Make `getContext` and `clearUser` static methods
  [Dave Perryman](https://github.com/Pezzah)
  [#132](https://github.com/bugsnag/bugsnag-android/pull/132)

* Ensure fatal crashes are sent as blocking requests
  [Simon Maynard](https://github.com/snmaynard)
  [#137](https://github.com/bugsnag/bugsnag-android/pull/137)

## 3.7.0 (2016-10-05)

### Enhancements

- Add support for sending reports using lambdas for customization
  [Delisa Mason](https://github.com/kattrali)
  [#123](https://github.com/bugsnag/bugsnag-android/pull/123)

## 3.6.0 (2016-09-09)

### Enhancements

- Support optionally persisting user information between sessions using the
  configuration option `persistUserBetweenSessions`
  [Dave Perryman](https://github.com/Pezzah)
  [#120](https://github.com/bugsnag/bugsnag-android/pull/120)

- Support initializing Bugsnag with a pre-configured Configuration instance
  [Dave Perryman](https://github.com/Pezzah)
  [#121](https://github.com/bugsnag/bugsnag-android/pull/121)

- Expose client context
  [nicous](https://github.com/nicous)
  [#112](https://github.com/bugsnag/bugsnag-android/pull/112)

- Add CPU/ABI information to device metadata
  [Dave Perryman](https://github.com/Pezzah)
  [Crossle Song](https://github.com/crossle)
  [#119](https://github.com/bugsnag/bugsnag-android/pull/119)

### Bug Fixes

- Fix potentially misdirected error report when changing the endpoint soon
  after initializing Bugsnag
  [Dave Perryman](https://github.com/Pezzah)
  [#121](https://github.com/bugsnag/bugsnag-android/pull/121)

- Fix missing static modifier on `disableExceptionHandler`
  [Niklas Klein](https://github.com/Taig)
  [#113](https://github.com/bugsnag/bugsnag-android/pull/113)

## 3.5.0 (2016-07-21)

### Enhancements

- Add access to new Breadcrumbs API
  [Delisa Mason](https://github.com/kattrali)
  [#111](https://github.com/bugsnag/bugsnag-android/pull/111)


## 3.4.0 (2016-03-09)

### Enhancements

- Limit the number of stored errors
  [Duncan Hewett](https://github.com/duncanhewett)
  [#97](https://github.com/bugsnag/bugsnag-android/pull/97)

### Bug Fixes

- Fix `ConcurrentModificationException` which could occur when saving
  breadcrumbs
  [Duncan Hewett](https://github.com/duncanhewett)
  [#98](https://github.com/bugsnag/bugsnag-android/pull/98)

- Localize all numbers in error metrics
  [Delisa Mason](https://github.com/kattrali)
  [#100](https://github.com/bugsnag/bugsnag-android/pull/100)

3.3.0 (2016-01-18)
-----

### Enhancements

- Change distribution method to be .aar only
  [Lars Grefer](https://github.com/larsgrefer)
  [#91](https://github.com/bugsnag/bugsnag-android/pull/91)

- Skip sending empty device data values
  [Matthias Urhahn](https://github.com/d4rken)
  [#96](https://github.com/bugsnag/bugsnag-android/pull/96)

- Remove the need for synthetic methods
  [Jake Wharton](https://github.com/JakeWharton)
  [#87](https://github.com/bugsnag/bugsnag-android/pull/87)

3.2.7 (2015-12-10)
-----

### Enhancements

- Add additional check to ensure the cache of uploaded errors are deleted
  [#80](https://github.com/bugsnag/bugsnag-android/issues/80)

### Bug Fixes

- Fix exception which occurs when `appContext.getResources()` is null
  [#78](https://github.com/bugsnag/bugsnag-android/issues/78)

- Fix bug preventing `maxBreadcrumbs` from being set
  [David Wu](https://github.com/wuman)
  [#70](https://github.com/bugsnag/bugsnag-android/pull/70)

3.2.6
-----
-   Add blocking API
-   Fix NPE issue
-   Concurrent adding to tabs
-   Thread Safe DateUtils#toISO8601

3.2.5
-----
-   Silence harmless proguard warning

3.2.4
-----
-   Support buildUUID to distinguish between multiple builds with the same appId and versionCode

3.2.3
-----
-   Support projectPackages when proguard is used.
-   Fix jailbroken % on Bugsnag dashboard.

3.2.2
-----
-   Prefer API keys passed to Client directly over those from AndroidManifest

3.2.1
-----
-   Fix NPE when unboxing in JsonStream (thanks @mattprecious)

3.2.0
-----
-   Allow setting Bugsnag API key in your AndroidManifest.xml

3.1.1
-----
-   Re-add `Error#getException` to allow access to exception in callbacks

3.1.0
-----
-   Add support for leaving developer-defined log messages called "breadcrumbs"
    to help understand what was happening in your application before each
    crash

3.0.0
-----
-   Removed dependency on `bugsnag-java`
-   Reduced memory usage, using lazy-loading and streaming
-   Easier "top activity" tracking
-   Device brand information is now collected (eg. "Samsung")
-   SSL enabled by default
-   Adding custom diagnostics (MetaData) is now easier to use
-   Fixed app name detection
-   Uses Android's new build system (gradle based)
-   Added unit tests, automatically run on travis.ci
-   Severity is now an Enum for type-safety

2.2.3
-----
-   Bump bugsnag-java dependency to fix prototype mismatch bug

2.2.2
-----
-   Add support for collecting thread-state information (enabled by default)

2.2.1
-----
-   Add support for beforeNotify callbacks
-   Allow disabling of automatic exception handler

2.2.0
-----
-   Add support for sending a custom app version with `setAppVersion`
-   Send both `versionName` and `versionCode` in the app tab

2.1.3
-----
-   Fix strictmode violation caused by hostname checking

2.1.2
-----
-   Prepare 'severity' feature for release

2.1.1
-----
-   Update bugsnag-java dependency, allows disabling of auto-notification

2.1.0
-----
-   Support severity
-   Better format of notification payload
-   Structure the data of notifications better

2.0.10
------
-   Fixed bug in `BugsnagFragmentActivity` calling the wrong callbacks.

2.0.9
-----
-   Added missing `setProjectPackages` and `setFilters` static methods
    to the `Bugsnag` class.

2.0.8
-----
-   Added additional `Activity` parent classes to help collect debug
    information, added support for custom Activities.

2.0.7
-----
-   Improved memory usage when sending exceptions that were previously
    saved to disk
-   `setNotifyReleaseStages` now defaults to `null`, to reduce confusion

2.0.6
-----
-   Fixed bug which caused notifications to be sent on the UI thread
    in some situations.
-   Fixed bug which meant `setIgnoreClasses` was not respected.

2.0.5
-----
-   Added support for `setIgnoreClasses` to set which exception classes
    should not be sent to Bugsnag.

2.0.4
-----
-   Attempt to automatically detect releaseStage from the debuggable flag
-   Android backward compatibility fixes, now works on Android 1.5+

2.0.3
-----
-   Fixed bug where session time wouldn't start counting until first exception

2.0.2
-----
-   Fixed missing apiKey in metrics, use java notifier's metrics sending

2.0.1
-----
-   Added `Bugsnag.addToTab` to replace `setExtraData` for sending meta-data
    with every exception
-   Reduced jar size

2.0.0
-----
-   Refactored to use the classes from `bugsnag-java`
-   Project is now available in Maven, and built using Maven
-   Added support for metrics tracking (MAU/DAU)
-   Added additional diagnostic information (network status, memory usage,
    gps status, time since boot, time since app load) to every notification
-   Fixed some issues which might have caused error sending to fail

1.0.2
-----
-   Ensure empty exception files aren't created

1.0.1
-----
-   Fix bug with incorrect "caused by" exception names and messages

1.0.0
-----
-   Initial release
