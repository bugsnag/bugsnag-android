package com.bugsnag.android.ndk;

import com.bugsnag.android.Breadcrumb;
import com.bugsnag.android.Configuration;
import com.bugsnag.android.MetaData;
import com.bugsnag.android.NativeInterface;

import android.os.Build;

import java.io.File;
import java.util.List;
import java.util.Observable;
import java.util.Observer;
import java.util.UUID;

/**
 * Observes changes in the Bugsnag environment, propagating them to the native layer
 */
public class NativeBridge implements Observer {
    static {
        System.loadLibrary("bugsnag-ndk");
    }

    private static final int METADATA_SECTION = 0;
    private static final int METADATA_KEY = 1;
    private static final int METADATA_VALUE = 2;

    public static native void install(String reportingDirectory, boolean autoNotify, int apiLevel);

    public static native void deliverReportAtPath(String filePath);

    public static native void addBreadcrumb(String name, String type, String timestamp,
                                            Object metadata);

    public static native void addMetadataString(String tab, String key, String value);

    public static native void addMetadataDouble(String tab, String key, double value);

    public static native void addMetadataBoolean(String tab, String key, boolean value);

    public static native void addHandledEvent();

    public static native void clearBreadcrumbs();

    public static native void clearMetadataTab(String tab);

    public static native void removeMetadata(String tab, String key);

    public static native void startedSession(String sessionID, String key);

    public static native void updateAppVersion(String appVersion);

    public static native void updateBuildUUID(String uuid);

    public static native void updateContext(String context);

    public static native void updateInForeground(boolean inForeground);

    public static native void updateLowMemory(boolean lowMemory);

    public static native void updateOrientation(String orientation);

    public static native void updateMetadata(Object metadata);

    public static native void updateReleaseStage(String releaseStage);

    public static native void updateUserId(String newValue);

    public static native void updateUserEmail(String newValue);

    public static native void updateUserName(String newValue);


    @Override
    public void update(Observable observable, Object rawMessage) {
        NativeInterface.Message message;
        Object arg = null;
        if (rawMessage instanceof NativeInterface.Message) {
            message = (NativeInterface.Message)rawMessage;
            arg = message.value;
        } else {
            return;
        }

        switch (message.type) {
            case INSTALL:
                if (arg instanceof Configuration) {
                    try {
                        File outFile = new File(NativeInterface.getNativeReportPath());
                        outFile.mkdirs();
                        for (final File file : outFile.listFiles()) {
                            deliverReportAtPath(file.getAbsolutePath());
                        }
                    } catch (Exception ex) {
                        // TODO: handle failure to create native crash report directory
                        return;
                    }

                    String reportPath = NativeInterface.getNativeReportPath()
                                        + UUID.randomUUID().toString() + ".crash";
                    install(reportPath, true, Build.VERSION.SDK_INT);
                }
                break;
            case ADD_BREADCRUMB:
                if (arg instanceof Breadcrumb) {
                    Breadcrumb crumb = (Breadcrumb) arg;
                    addBreadcrumb(crumb.getName(), crumb.getType().toString(),
                                  crumb.getTimestamp(), crumb.getMetadata());
                }
                break;
            case ADD_METADATA:
                if (arg instanceof List) {
                    @SuppressWarnings("unchecked")
                    List<Object> values = (List<Object>) arg;
                    if (values.size() == 3 && values.get(METADATA_SECTION) instanceof String
                            && values.get(METADATA_KEY) instanceof String) {
                        if (values.get(METADATA_VALUE) instanceof String) {
                            addMetadataString((String) values.get(METADATA_SECTION),
                                    (String) values.get(METADATA_KEY),
                                    (String) values.get(METADATA_VALUE));
                        } else if (values.get(METADATA_VALUE) instanceof Boolean) {
                            addMetadataBoolean((String) values.get(METADATA_SECTION),
                                    (String) values.get(METADATA_KEY),
                                    (Boolean) values.get(METADATA_VALUE));
                        } else if (values.get(METADATA_VALUE) instanceof Number) {
                            addMetadataDouble((String) values.get(METADATA_SECTION),
                                    (String) values.get(METADATA_KEY),
                                    ((Number) values.get(METADATA_VALUE)).doubleValue());
                        }
                    } else if (values.size() == 2) {
                        removeMetadata((String)values.get(METADATA_SECTION),
                                       (String)values.get(METADATA_KEY));
                    }
                }
                break;
            case CLEAR_BREADCRUMBS:
                clearBreadcrumbs();
                break;
            case CLEAR_METADATA_TAB:
                if (arg instanceof String) {
                    clearMetadataTab((String)arg);
                }
                break;
            case NOTIFY_HANDLED:
                addHandledEvent();
                break;
            case REMOVE_METADATA:
                if (arg instanceof List) {
                    @SuppressWarnings("unchecked")
                    List<String> metadata = (List<String>)arg;
                    if (metadata.size() == 2) {
                        removeMetadata(metadata.get(METADATA_SECTION),
                                       metadata.get(METADATA_KEY));
                    }
                }
                break;
            case START_SESSION:
                if (arg instanceof List) {
                    @SuppressWarnings("unchecked")
                    List<String> metadata = (List<String>)arg;
                    if (metadata.size() == 2) {
                        startedSession(metadata.get(0), metadata.get(1));
                    }
                }
                break;
            case UPDATE_APP_VERSION:
                if (arg instanceof String) {
                    updateAppVersion((String)arg);
                }
                break;
            case UPDATE_BUILD_UUID:
                updateBuildUUID(arg == null ? "" : (String)arg);
                break;
            case UPDATE_CONTEXT:
                updateContext(arg == null ? "" : (String)arg);
                break;
            case UPDATE_IN_FOREGROUND:
                if (arg instanceof Boolean) {
                    updateInForeground((Boolean)arg);
                }
                break;
            case UPDATE_LOW_MEMORY:
                if (arg instanceof Boolean) {
                    updateLowMemory((Boolean)arg);
                }
                break;
            case UPDATE_METADATA:
                if (arg instanceof MetaData) {
                    updateMetadata(arg);
                }
                break;
            case UPDATE_ORIENTATION:
                updateOrientation(arg == null ? "" : (String)arg);
                break;
            case UPDATE_RELEASE_STAGE:
                if (arg instanceof String) {
                    updateReleaseStage((String)arg);
                }
                break;
            case UPDATE_USER_ID:
                updateUserId(arg == null ? "" : (String)arg);
                break;
            case UPDATE_USER_NAME:
                updateUserName(arg == null ? "" : (String)arg);
                break;
            case UPDATE_USER_EMAIL:
                updateUserEmail(arg == null ? "" : (String)arg);
                break;
            default:
        }
    }
}
