# Performance benchmarks

This contains baseline performance benchmarks for Bugsnag's API from the
[microbenchmark library](https://developer.android.com/studio/profile/benchmark).

These are collected manually from the [Browserstack run](https://app-automate.browserstack.com/dashboard/v2/builds/0ff96bfc50e2317f926b7fa6a4d59592f6c7862f).
This was last collected from the [following commit](https://github.com/bugsnag/bugsnag-android/pull/1273/commits/9c8fc223fbdda9f9f12e493b3918af0105ea21bd).

## Caveats

The following factors introduce variance to some of the benchmarking results:

- Some APIs use I/O which can have high variance
- Some functions use one-off initialization or caching
- Clock locking is not enabled as this would require a rooted device, this decreases the accuracy of the benchmarking library

It is therefore important to compare results against a previous baseline rather than looking at the absolute values.

## Results

6,262 ns ClientDataBenchmarkTest.setContext
183,333 ns ClientDataBenchmarkTest.setUser
1,615 ns SessionBenchmarkTest.pauseSession
42,349 ns SessionBenchmarkTest.resumeSession
104,115 ns SessionBenchmarkTest.startSession
3,201 ns CriticalPathBenchmarkTest.addMetadataSection
10,108 ns CriticalPathBenchmarkTest.addSingleMetadataValue
6,526 ns CriticalPathBenchmarkTest.clearMetadataSection
9,099 ns CriticalPathBenchmarkTest.clearSingleMetadataValue
4,449,375 ns CriticalPathBenchmarkTest.clientNotify
127,378 ns CriticalPathBenchmarkTest.configConstructor
835,261 ns CriticalPathBenchmarkTest.configManifestLoad
190 ns CriticalPathBenchmarkTest.getMetadataSection
367 ns CriticalPathBenchmarkTest.getSingleMetadataValue
166,458 ns CriticalPathBenchmarkTest.leaveComplexBreadcrumb
162,916 ns CriticalPathBenchmarkTest.leaveSimpleBreadcrumb
1,552,396 ns EventBenchmarkTest.createEvent
