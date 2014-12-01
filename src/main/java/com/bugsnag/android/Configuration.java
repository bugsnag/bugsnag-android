package com.bugsnag.android;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

class Configuration {
    static final String DEFAULT_ENDPOINT = "http://notify.bugsnag.com";

    String apiKey;
    String appVersion;
    String context;
    String endpoint = DEFAULT_ENDPOINT;
    String[] filters = new String[]{"password"};
    String[] ignoreClasses;
    String[] notifyReleaseStages = null;
    String[] projectPackages;
    String releaseStage;
    boolean sendThreads = true;

    MetaData metaData = new MetaData();
    Collection<BeforeNotify> beforeNotifyTasks = new LinkedList<BeforeNotify>();

    Configuration(String apiKey) {
        this.apiKey = apiKey;
    }

    String getNotifyEndpoint() {
        return endpoint;
    }

    String getAnalyticsEndpoint() {
        return String.format("%s/metrics", endpoint);
    }

    boolean shouldNotifyForReleaseStage(String releaseStage) {
        if(this.notifyReleaseStages == null)
            return true;

        List<String> stages = Arrays.asList(this.notifyReleaseStages);
        return stages.contains(releaseStage);
    }

    boolean shouldIgnoreClass(String className) {
        if(this.ignoreClasses == null)
            return false;

        List<String> classes = Arrays.asList(this.ignoreClasses);
        return classes.contains(className);
    }

    void addBeforeNotify(BeforeNotify beforeNotify) {
        this.beforeNotifyTasks.add(beforeNotify);
    }

    boolean inProject(String className) {
        if(projectPackages != null) {
            for(String packageName : projectPackages) {
                if(packageName != null && className.startsWith(packageName)) {
                    return true;
                }
            }
        }

        return false;
    }
}
