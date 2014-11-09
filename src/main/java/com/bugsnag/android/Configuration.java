package com.bugsnag.android;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

class Configuration {
    static final String DEFAULT_ENDPOINT = "http://notify.bugsnag.com";

    String apiKey;
    String appVersion;
    boolean autoNotify = true;
    String context;
    String endpoint = DEFAULT_ENDPOINT;
    String[] filters = new String[]{"password"};
    String[] ignoreClasses;
    String[] notifyReleaseStages = null;
    String[] projectPackages;
    String releaseStage;
    boolean sendThreads = true;

    MetaData metaData;
    List<BeforeNotify> beforeNotifyTasks = new LinkedList<BeforeNotify>();

    Configuration(String apiKey) {
        this.apiKey = apiKey;
    }

    String getNotifyEndpoint() {
        return endpoint;
    }

    String getMetricsEndpoint() {
        return String.format("%s/metrics", endpoint);
    }

    void addToTab(String tabName, String key, Object value) {
        metaData.addToTab(tabName, key, value);
    }

    void clearTab(String tabName){
        metaData.clearTab(tabName);
    }

    boolean shouldNotify() {
        if(this.notifyReleaseStages == null)
            return true;

        List<String> stages = Arrays.asList(this.notifyReleaseStages);
        return stages.contains(this.releaseStage);
    }

    boolean shouldIgnore(String className) {
        if(this.ignoreClasses == null)
            return false;

        List<String> classes = Arrays.asList(this.ignoreClasses);
        return classes.contains(className);
    }

    boolean runBeforeNotify(Error error) {
        for (BeforeNotify beforeNotify : beforeNotifyTasks) {
            try {
                if (!beforeNotify.run(error)) {
                    return false;
                }
            } catch (Throwable ex) {
                Logger.warn("BeforeNotify threw an Exception", ex);
            }
        }

        // By default, allow the error to be sent if there were no objections
        return true;
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
