package com.bugsnag.android.internal.journal

internal object JournalKeys {
    // Keys
    internal const val keyApp = "app"
    internal const val keyAttributes = "attributes"
    internal const val keyBinaryArch = "binaryArch"
    internal const val keyBuildUUID = "buildUUID"
    internal const val keyCode = "code"
    internal const val keyCodeBundleId = "codeBundleId"
    internal const val keyColumnNumber = "columnNumber"
    internal const val keyCpuAbi = "cpuAbi"
    internal const val keyDependencies = "dependencies"
    internal const val keyDevice = "device"
    internal const val keyDuration = "duration"
    internal const val keyDurationInFG = "durationInForeground"
    internal const val keyEmail = "email"
    internal const val keyErrorReportingThread = "errorReportingThread"
    internal const val keyEvents = "events"
    internal const val keyFile = "file"
    internal const val keyFrameAddress = "frameAddress"
    internal const val keyFreeDisk = "freeDisk"
    internal const val keyFreeMemory = "freeMemory"
    internal const val keyHandled = "handled"
    internal const val keyId = "id"
    internal const val keyInForeground = "inForeground"
    internal const val keyInProject = "inProject"
    internal const val keyIsLaunching = "isLaunching"
    internal const val keyIsPC = "isPC"
    internal const val keyJailbroken = "jailbroken"
    internal const val keyLineNumber = "lineNumber"
    internal const val keyLoadAddress = "loadAddress"
    internal const val keyLocale = "locale"
    internal const val keyManufacturer = "manufacturer"
    internal const val keyMetadata = "metaData"
    internal const val keyMethod = "method"
    internal const val keyModel = "model"
    internal const val keyName = "name"
    internal const val keyOrientation = "orientation"
    internal const val keyOSName = "osName"
    internal const val keyOSVersion = "osVersion"
    internal const val keyReleaseStage = "releaseStage"
    internal const val keyRuntimeVersions = "runtimeVersions"
    internal const val keyStackTrace = "stacktrace"
    internal const val keyStartedAt = "startedAt"
    internal const val keyState = "state"
    internal const val keySymbolAddress = "symbolAddress"
    internal const val keyTime = "time"
    internal const val keyTimeEnteredForeground = "timeEnteredForeground"
    internal const val keyTimeLaunched = "timeLaunched"
    internal const val keyTimeNow = "timeNow"
    internal const val keyTimestamp = "timestamp"
    internal const val keyTotalMemory = "totalMemory"
    internal const val keyType = "type"
    internal const val keyUnhandled = "unhandled"
    internal const val keyUnhandledOverridden = "unhandledOverridden"
    internal const val keyUrl = "url"
    internal const val keyVersion = "version"
    internal const val keyVersionCode = "versionCode"
    internal const val keyVersionInfo = "version-info"

    // Primary paths
    internal const val pathApiKey = "apiKey"
    internal const val pathApp = keyApp
    internal const val pathBreadcrumbs = "breadcrumbs"
    internal const val pathContext = "context"
    internal const val pathDevice = keyDevice
    internal const val pathExceptions = "exceptions"
    internal const val pathGroupingHash = "groupingHash"
    internal const val pathMetadata = keyMetadata
    internal const val pathRuntime = "runtime"
    internal const val pathSession = "session"
    internal const val pathSeverity = "severity"
    internal const val pathSeverityReason = "severityReason"
    internal const val pathThreads = "threads"
    internal const val pathUser = "user"
    internal const val pathNotifier = "notifier"
    internal const val pathProjectPackages = "projectPackages"

    // Composite paths
    internal const val pathAppInForeground = "$pathApp.$keyInForeground"
    internal const val pathAppIsLaunching = "$pathApp.$keyIsLaunching"
    internal const val pathDeviceOrientation = "$pathDevice.orientation"
    internal const val pathMetadataApp = "$pathMetadata.$keyApp"
    internal const val pathMetadataAppActiveScreen = "$pathMetadataApp.activeScreen"
    internal const val pathMetadataAppLowMemory = "$pathMetadataApp.lowMemory"
    internal const val pathMetadataAppMemoryTrimLevel = "$pathMetadataApp.memoryTrimLevel"
    internal const val pathMetadataAppNetworkAccess = "$pathMetadataApp.networkAccess"
    internal const val pathMetadataDevice = "$pathMetadata.$keyDevice"
    internal const val pathRuntimeEnteredForegroundTime = "$pathRuntime.$keyTimeEnteredForeground"
    internal const val pathRuntimeLaunchTime = "$pathRuntime.$keyTimeLaunched"
    internal const val pathRuntimeNowTime = "$pathRuntime.$keyTimeNow"
    internal const val pathSessionEvents = "$pathSession.events"
    internal const val pathSessionEventsHandled = "$pathSessionEvents.handled"
    internal const val pathSessionEventsUnhandled = "$pathSessionEvents.unhandled"
}
