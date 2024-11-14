package com.bugsnag.android;

import com.bugsnag.android.internal.ImmutableConfig;
import com.bugsnag.android.internal.dag.Provider;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

class ConfigSerializer implements MapSerializer<ImmutableConfig> {

    @Override
    public void serialize(Map<String, Object> map, ImmutableConfig config) {
        map.put("apiKey", config.getApiKey());
        map.put("autoDetectErrors", config.getAutoDetectErrors());
        map.put("autoTrackSessions", config.getAutoTrackSessions());
        map.put("sendThreads", config.getSendThreads().toString());
        map.put("projectPackages", config.getProjectPackages());
        map.put("enabledReleaseStages", config.getEnabledReleaseStages());
        map.put("releaseStage", config.getReleaseStage());
        Provider<String> buildUuid = config.getBuildUuid();
        if (buildUuid != null) {
            map.put("buildUuid", buildUuid.getOrNull());
        }
        if (config.getAppVersion() != null) {
            map.put("appVersion", config.getAppVersion());
        }
        map.put("versionCode", config.getVersionCode());
        map.put("type", config.getAppType());
        map.put("persistUser", config.getPersistUser());
        map.put("generateAnonymousId", config.getGenerateAnonymousId());
        map.put("launchCrashThresholdMs", (int) config.getLaunchDurationMillis());
        map.put("maxBreadcrumbs", config.getMaxBreadcrumbs());
        map.put("enabledBreadcrumbTypes", serializeBreadrumbTypes(config));
        map.put("enabledErrorTypes", serializeErrorTypes(config));
        map.put("endpoints", serializeEndpoints(config));
    }

    private Collection<String> serializeBreadrumbTypes(ImmutableConfig config) {
        Set<BreadcrumbType> types = config.getEnabledBreadcrumbTypes();
        if (types == null) {
            return null;
        }
        Collection<String> crumbTypes = new HashSet<>();
        for (BreadcrumbType type : types) {
            crumbTypes.add(type.toString());
        }
        return crumbTypes;
    }

    private Map<String, Object> serializeErrorTypes(ImmutableConfig config) {
        Map<String, Object> map = new HashMap<>();
        ErrorTypes errorTypes = config.getEnabledErrorTypes();
        map.put("anrs", errorTypes.getAnrs());
        map.put("ndkCrashes", errorTypes.getNdkCrashes());
        map.put("unhandledExceptions", errorTypes.getUnhandledExceptions());
        map.put("unhandledRejections", errorTypes.getUnhandledRejections());
        return map;
    }

    private Map<String, Object> serializeEndpoints(ImmutableConfig config) {
        Map<String, Object> map = new HashMap<>();
        EndpointConfiguration endpoints = config.getEndpoints();
        map.put("notify", endpoints.getNotify());
        map.put("sessions", endpoints.getSessions());
        return map;
    }
}
