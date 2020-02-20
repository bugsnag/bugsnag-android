package com.bugsnag.android;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

@SuppressWarnings("ConstantConditions") // suppress warning about making redundant null checks
public class Configuration implements CallbackAware, MetadataAware, UserAware {

    final ConfigImpl impl;

    public Configuration(@NonNull String apiKey) {
        impl = new ConfigImpl(apiKey);
    }

    @NonNull
    public static Configuration load(@NonNull Context context) {
        return ConfigImpl.load(context);
    }

    @NonNull
    static Configuration load(@NonNull Context context, @NonNull String apiKey) {
        return ConfigImpl.load(context, apiKey);
    }

    private void warn(String property) {
        getLogger().w("Invalid null value supplied to config." + property + ", ignoring");
    }

    @NonNull
    public String getApiKey() {
        return impl.getApiKey();
    }

    public void setApiKey(@NonNull String apiKey) {
        if (apiKey != null) {
            impl.setApiKey(apiKey);
        } else {
            warn("apiKey");
        }
    }

    @Nullable
    public String getBuildUuid() {
        return impl.getBuildUuid();
    }

    public void setBuildUuid(@Nullable String buildUuid) {
        impl.setBuildUuid(buildUuid);
    }

    @Nullable
    public String getAppVersion() {
        return impl.getAppVersion();
    }

    public void setAppVersion(@Nullable String appVersion) {
        impl.setAppVersion(appVersion);
    }

    @Nullable
    public Integer getVersionCode() {
        return impl.getVersionCode();
    }

    public void setVersionCode(@Nullable Integer versionCode) {
        impl.setVersionCode(versionCode);
    }

    @Nullable
    public String getReleaseStage() {
        return impl.getReleaseStage();
    }

    public void setReleaseStage(@Nullable String releaseStage) {
        impl.setReleaseStage(releaseStage);
    }

    @NonNull
    public Thread.ThreadSendPolicy getSendThreads() {
        return impl.getSendThreads();
    }

    public void setSendThreads(@NonNull Thread.ThreadSendPolicy sendThreads) {
        if (sendThreads != null) {
            impl.setSendThreads(sendThreads);
        } else {
            warn("sendThreads");
        }
    }

    public boolean getPersistUser() {
        return impl.getPersistUser();
    }

    public void setPersistUser(boolean persistUser) {
        impl.setPersistUser(persistUser);
    }

    public long getLaunchCrashThresholdMs() {
        return impl.getLaunchCrashThresholdMs();
    }

    public void setLaunchCrashThresholdMs(long launchCrashThresholdMs) {
        impl.setLaunchCrashThresholdMs(launchCrashThresholdMs);
    }

    public boolean getAutoTrackSessions() {
        return impl.getAutoTrackSessions();
    }

    public void setAutoTrackSessions(boolean autoTrackSessions) {
        impl.setAutoTrackSessions(autoTrackSessions);
    }

    @NonNull
    public ErrorTypes getEnabledErrorTypes() {
        return impl.getEnabledErrorTypes();
    }

    public void setEnabledErrorTypes(@NonNull ErrorTypes enabledErrorTypes) {
        if (enabledErrorTypes != null) {
            impl.setEnabledErrorTypes(enabledErrorTypes);
        } else {
            warn("enabledErrorTypes");
        }
    }

    public boolean getAutoDetectErrors() {
        return impl.getAutoDetectErrors();
    }

    public void setAutoDetectErrors(boolean autoDetectErrors) {
        impl.setAutoDetectErrors(autoDetectErrors);
    }

    @Nullable
    public String getCodeBundleId() {
        return impl.getCodeBundleId();
    }

    public void setCodeBundleId(@Nullable String codeBundleId) {
        impl.setCodeBundleId(codeBundleId);
    }

    @Nullable
    public String getAppType() {
        return impl.getAppType();
    }

    public void setAppType(@Nullable String appType) {
        impl.setAppType(appType);
    }

    @Nullable
    public Logger getLogger() {
        return impl.getLogger();
    }

    public void setLogger(@Nullable Logger logger) {
        impl.setLogger(logger);
    }

    @NonNull
    public Delivery getDelivery() {
        return impl.getDelivery();
    }

    public void setDelivery(@NonNull Delivery delivery) {
        if (delivery != null) {
            impl.setDelivery(delivery);
        } else {
            warn("delivery");
        }
    }

    @Nullable
    public EndpointConfiguration getEndpoints() {
        return impl.getEndpoints();
    }

    public void setEndpoints(@Nullable EndpointConfiguration endpoints) {
        if (endpoints != null) {
            impl.setEndpoints(endpoints);
        } else {
            impl.setEndpoints(new EndpointConfiguration());
        }
    }

    public int getMaxBreadcrumbs() {
        return impl.getMaxBreadcrumbs();
    }

    public void setMaxBreadcrumbs(int maxBreadcrumbs) {
        impl.setMaxBreadcrumbs(maxBreadcrumbs);
    }

    @Nullable
    public String getContext() {
        return impl.getContext();
    }

    public void setContext(@Nullable String context) {
        impl.setContext(context);
    }

    @Nullable
    public Set<String> getRedactedKeys() {
        return impl.getRedactedKeys();
    }

    public void setRedactedKeys(@Nullable Set<String> redactedKeys) {
        if (redactedKeys != null) {
            impl.setRedactedKeys(redactedKeys);
        } else {
            impl.setRedactedKeys(Collections.<String>emptySet());
        }
    }

    @Nullable
    public Set<String> getDiscardClasses() {
        return impl.getDiscardClasses();
    }

    public void setDiscardClasses(@Nullable Set<String> discardClasses) {
        if (discardClasses != null) {
            impl.setDiscardClasses(discardClasses);
        } else {
            impl.setDiscardClasses(Collections.<String>emptySet());
        }
    }

    @Nullable
    public Set<String> getEnabledReleaseStages() {
        return impl.getEnabledReleaseStages();
    }

    public void setEnabledReleaseStages(@Nullable Set<String> enabledReleaseStages) {
        impl.setEnabledReleaseStages(enabledReleaseStages);
    }

    @Nullable
    public Set<BreadcrumbType> getEnabledBreadcrumbTypes() {
        return impl.getEnabledBreadcrumbTypes();
    }

    public void setEnabledBreadcrumbTypes(@Nullable Set<BreadcrumbType> enabledBreadcrumbTypes) {
        impl.setEnabledBreadcrumbTypes(enabledBreadcrumbTypes);
    }

    @NonNull
    public Set<String> getProjectPackages() {
        return impl.getProjectPackages();
    }

    public void setProjectPackages(@NonNull Set<String> projectPackages) {
        if (projectPackages != null) {
            impl.setProjectPackages(projectPackages);
        } else {
            warn("projectPackages");
        }
    }

    @Override
    public void addOnError(@NonNull OnErrorCallback onError) {
        if (onError != null) {
            impl.addOnError(onError);
        } else {
            warn("addOnError");
        }
    }

    @Override
    public void removeOnError(@NonNull OnErrorCallback onError) {
        if (onError != null) {
            impl.removeOnError(onError);
        } else {
            warn("removeOnError");
        }
    }

    @Override
    public void addOnBreadcrumb(@NonNull OnBreadcrumbCallback onBreadcrumb) {
        if (onBreadcrumb != null) {
            impl.addOnBreadcrumb(onBreadcrumb);
        } else {
            warn("addOnBreadcrumb");
        }
    }

    @Override
    public void removeOnBreadcrumb(@NonNull OnBreadcrumbCallback onBreadcrumb) {
        if (onBreadcrumb != null) {
            impl.removeOnBreadcrumb(onBreadcrumb);
        } else {
            warn("removeOnBreadcrumb");
        }
    }

    @Override
    public void addOnSession(@NonNull OnSessionCallback onSession) {
        if (onSession != null) {
            impl.addOnSession(onSession);
        } else {
            warn("addOnSession");
        }
    }

    @Override
    public void removeOnSession(@NonNull OnSessionCallback onSession) {
        if (onSession != null) {
            impl.removeOnSession(onSession);
        } else {
            warn("removeOnSession");
        }
    }

    @Override
    public void addMetadata(@NonNull String section, @NonNull Map<String, ?> value) {
        if (section != null && value != null) {
            impl.addMetadata(section, value);
        } else {
            warn("addMetadata");
        }
    }

    @Override
    public void addMetadata(@NonNull String section, @NonNull String key, @Nullable Object value) {
        if (section != null && key != null) {
            impl.addMetadata(section, key, value);
        } else {
            warn("addMetadata");
        }
    }

    @Override
    public void clearMetadata(@NonNull String section) {
        if (section != null) {
            impl.clearMetadata(section);
        } else {
            warn("clearMetadata");
        }
    }

    @Override
    public void clearMetadata(@NonNull String section, @NonNull String key) {
        if (section != null && key != null) {
            impl.clearMetadata(section, key);
        } else {
            warn("clearMetadata");
        }
    }

    @Nullable
    @Override
    public Map<String, Object> getMetadata(@NonNull String section) {
        if (section != null) {
            return impl.getMetadata(section);
        } else {
            warn("getMetadata");
            return null;
        }
    }

    @Nullable
    @Override
    public Object getMetadata(@NonNull String section, @NonNull String key) {
        if (section != null && key != null) {
            return impl.getMetadata(section, key);
        } else {
            warn("getMetadata");
            return null;
        }
    }

    @NonNull
    @Override
    public User getUser() {
        return impl.getUser();
    }

    @Override
    public void setUser(@Nullable String id, @Nullable String email, @Nullable String name) {
        impl.setUser(id, email, name);
    }
}
