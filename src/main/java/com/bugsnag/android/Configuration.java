package com.bugsnag.android;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

class Configuration {
    static final String DEFAULT_ENDPOINT = "http://notify.bugsnag.com";

    static final String NOTIFIER_NAME = "Android Bugsnag Notifier";
    static final String NOTIFIER_VERSION = "3.0.0";
    static final String NOTIFIER_URL = "https://bugsnag.com";

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

    public Configuration(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getNotifyEndpoint() {
        return endpoint;
    }

    public String getMetricsEndpoint() {
        return String.format("%s/metrics", endpoint);
    }

    public void addToTab(String tabName, String key, Object value) {
        metaData.addToTab(tabName, key, value);
    }

    public void clearTab(String tabName){
        metaData.clearTab(tabName);
    }

    public boolean shouldNotify() {
        if(this.notifyReleaseStages == null)
            return true;

        List<String> stages = Arrays.asList(this.notifyReleaseStages);
        return stages.contains(this.releaseStage);
    }

    public boolean shouldIgnore(String className) {
        if(this.ignoreClasses == null)
            return false;

        List<String> classes = Arrays.asList(this.ignoreClasses);
        return classes.contains(className);
    }

    public boolean runBeforeNotify(Error error) {
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

    public void setUser(String id, String email, String name) {
        // TODO
    }

    public void addBeforeNotify(BeforeNotify beforeNotify) {
        this.beforeNotifyTasks.add(beforeNotify);
    }
}
