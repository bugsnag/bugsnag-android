# Performance benchmarks

This contains baseline performance benchmarks for Bugsnag's API from the
[microbenchmark library](https://developer.android.com/studio/profile/benchmark).

These are collected manually from the [Browserstack run](https://app-automate.browserstack.com/dashboard/v2/builds/3953b399be9a856b01bcd675f6b6854542f55405).
This was last collected from the [following commit](https://github.com/bugsnag/bugsnag-android/pull/1273/commits/94f288c706abf6ef6773439188ec4d09c6fcfe36).

## Caveats

The following factors introduce variance to some of the benchmarking results:

- Some APIs use I/O which can have high variance
- Some functions use one-off initialization or caching
- Clock locking is not enabled as this would require a rooted device, this decreases the accuracy of the benchmarking library

It is therefore important to compare results against a previous baseline rather than looking at the absolute values.

## Results

3,395 ns CriticalPathBenchmarkTest.addMetadataSection
10,679 ns CriticalPathBenchmarkTest.addSingleMetadataValue
7,028 ns CriticalPathBenchmarkTest.clearMetadataSection
10,118 ns CriticalPathBenchmarkTest.clearSingleMetadataValue
4,649,115 ns CriticalPathBenchmarkTest.clientNotify
140,625 ns CriticalPathBenchmarkTest.configConstructor
952,448 ns CriticalPathBenchmarkTest.configManifestLoad
190 ns CriticalPathBenchmarkTest.getMetadataSection
351 ns CriticalPathBenchmarkTest.getSingleMetadataValue
163,021 ns CriticalPathBenchmarkTest.leaveComplexBreadcrumb
426,666 ns CriticalPathBenchmarkTest.leaveSimpleBreadcrumb
1,456,772 ns JsonSerializationBenchmarkTest.serializeEventPayload
93,932 ns JsonSerializationBenchmarkTest.serializeSessionPayload
1,631 ns SessionBenchmarkTest.pauseSession
40,355 ns SessionBenchmarkTest.resumeSession
126,563 ns SessionBenchmarkTest.startSession
6,725 ns ClientDataBenchmarkTest.setContext
176,953 ns ClientDataBenchmarkTest.setUser
1,525,417 ns EventBenchmarkTest.createEvent
