package com.bugsnag.android.ndk;

import com.bugsnag.android.Breadcrumb;
import com.bugsnag.android.MetaData;
import com.bugsnag.android.NativeInterface;

import android.os.Build;
import android.support.annotation.Nullable;
import android.util.Log;

import java.io.File;
import java.util.List;
import java.util.Observable;
import java.util.Observer;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

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
    private static final String LOG_TAG = "BugsnagNDK:NativeBridge";
    private static final Lock lock = new ReentrantLock();
    private static final AtomicBoolean installed = new AtomicBoolean(false);

    public static native void install(String reportingDirectory, boolean autoNotify, int apiLevel,
                                      boolean is32bit);

    public static native void deliverReportAtPath(String filePath);

    public static native void addBreadcrumb(String name, String type, String timestamp,
                                            Object metadata);

    public static native void addMetadataString(String tab, String key, String value);

    public static native void addMetadataDouble(String tab, String key, double value);

    public static native void addMetadataBoolean(String tab, String key, boolean value);

    public static native void addHandledEvent();

    public static native void addUnhandledEvent();

    public static native void clearBreadcrumbs();

    public static native void clearMetadataTab(String tab);

    public static native void removeMetadata(String tab, String key);

    public static native void startedSession(String sessionID, String key,
                                             int handledCount, int unhandledCount);

    public static native void stoppedSession();

    public static native void updateAppVersion(String appVersion);

    public static native void updateBuildUUID(String uuid);

    public static native void updateContext(String context);

    public static native void updateInForeground(boolean inForeground, String activityName);

    public static native void updateLowMemory(boolean lowMemory);

    public static native void updateOrientation(int orientation);

    public static native void updateMetadata(Object metadata);

    public static native void updateReleaseStage(String releaseStage);

    public static native void updateUserId(String newValue);

    public static native void updateUserEmail(String newValue);

    public static native void updateUserName(String newValue);

    private boolean loggingEnabled = true;
    private final String reportDirectory;

    /**
     * Creates a new native bridge for interacting with native components.
     * Configures logging and ensures that the reporting directory exists
     * immediately.
     */
    public NativeBridge() {
        loggingEnabled = NativeInterface.getLoggingEnabled();
        reportDirectory = NativeInterface.getNativeReportPath();
        File outFile = new File(reportDirectory);
        if (!outFile.exists() && !outFile.mkdirs()) {
            warn("The native reporting directory cannot be created.");
        }
    }

    @Override
    public void update(Observable observable, Object rawMessage) {
        NativeInterface.Message message = parseMessage(rawMessage);
        if (message == null) {
            return;
        }
        Object arg = message.value;

        switch (message.type) {
            case INSTALL:
                handleInstallMessage(arg);
                break;
            case DELIVER_PENDING:
                deliverPendingReports();
                break;
            case ADD_BREADCRUMB:
                handleAddBreadcrumb(arg);
                break;
            case ADD_METADATA:
                handleAddMetadata(arg);
                break;
            case CLEAR_BREADCRUMBS:
                clearBreadcrumbs();
                break;
            case CLEAR_METADATA_TAB:
                handleClearMetadataTab(arg);
                break;
            case NOTIFY_HANDLED:
                addHandledEvent();
                break;
            case NOTIFY_UNHANDLED:
                addUnhandledEvent();
                break;
            case REMOVE_METADATA:
                handleRemoveMetadata(arg);
                break;
            case START_SESSION:
                handleStartSession(arg);
                break;
            case STOP_SESSION:
                stoppedSession();
                break;
            case UPDATE_APP_VERSION:
                handleAppVersionChange(arg);
                break;
            case UPDATE_BUILD_UUID:
                handleBuildUUIDChange(arg);
                break;
            case UPDATE_CONTEXT:
                handleContextChange(arg);
                break;
            case UPDATE_IN_FOREGROUND:
                handleForegroundActivityChange(arg);
                break;
            case UPDATE_LOW_MEMORY:
                handleLowMemoryChange(arg);
                break;
            case UPDATE_METADATA:
                handleUpdateMetadata(arg);
                break;
            case UPDATE_ORIENTATION:
                handleOrientationChange(arg);
                break;
            case UPDATE_RELEASE_STAGE:
                handleReleaseStageChange(arg);
                break;
            case UPDATE_USER_ID:
                handleUserIdChange(arg);
                break;
            case UPDATE_USER_NAME:
                handleUserNameChange(arg);
                break;
            case UPDATE_USER_EMAIL:
                handleUserEmailChange(arg);
                break;
            default:
        }
    }

    @Nullable
    private NativeInterface.Message parseMessage(Object rawMessage) {
        if (rawMessage instanceof NativeInterface.Message) {
            NativeInterface.Message message = (NativeInterface.Message)rawMessage;
            if (message.type != NativeInterface.MessageType.INSTALL && !installed.get()) {
                warn("Received message before INSTALL: " + message.type);
                return null;
            }
            return message;
        } else {
            if (rawMessage == null) {
                warn("Received observable update with null Message");
            } else {
                warn("Received observable update object which is not instance of Message: "
                    + rawMessage.getClass());
            }
            return null;
        }
    }

    private void deliverPendingReports() {
        lock.lock();
        try {
            File outDir = new File(reportDirectory);
            if (outDir.exists()) {
                File[] fileList = outDir.listFiles();
                if (fileList != null) {
                    for (final File file : fileList) {
                        deliverReportAtPath(file.getAbsolutePath());
                    }
                }
            } else {
                warn("Report directory does not exist, cannot read pending reports");
            }
        } catch (Exception ex) {
            warn("Failed to parse/write pending reports: " + ex);
        } finally {
            lock.unlock();
        }
    }

    private void handleInstallMessage(Object arg) {
        lock.lock();
        try {
            if (installed.get()) {
                warn("Received duplicate setup message with arg: " + arg);
                return;
            }
            String reportPath = reportDirectory + UUID.randomUUID().toString() + ".crash";
            install(reportPath, true, Build.VERSION.SDK_INT, is32bit());
            installed.set(true);
        } finally {
            lock.unlock();
        }
    }

    private boolean is32bit() {
        String[] abis = NativeInterface.getCpuAbi();

        boolean is32bit = true;
        for (String abi : abis) {
            if (abi.contains("64")) {
                is32bit = false;
                break;
            }
        }
        return is32bit;
    }

    private void handleAddBreadcrumb(Object arg) {
        if (arg instanceof Breadcrumb) {
            Breadcrumb crumb = (Breadcrumb) arg;
            addBreadcrumb(crumb.getName(), crumb.getType().toString(),
                crumb.getTimestamp(), crumb.getMetadata());
        } else {
            warn("Attempted to add non-breadcrumb: " + arg);
        }
    }

    private void handleAddMetadata(Object arg) {
        if (arg instanceof List) {
            @SuppressWarnings("unchecked")
            List<Object> values = (List<Object>) arg;
            if (values.size() == 3 && values.get(METADATA_SECTION) instanceof String
                && values.get(METADATA_KEY) instanceof String) {
                if (values.get(METADATA_VALUE) instanceof String) {
                    addMetadataString((String) values.get(METADATA_SECTION),
                        (String) values.get(METADATA_KEY),
                        (String) values.get(METADATA_VALUE));
                    return;
                } else if (values.get(METADATA_VALUE) instanceof Boolean) {
                    addMetadataBoolean((String) values.get(METADATA_SECTION),
                        (String) values.get(METADATA_KEY),
                        (Boolean) values.get(METADATA_VALUE));
                    return;
                } else if (values.get(METADATA_VALUE) instanceof Number) {
                    addMetadataDouble((String) values.get(METADATA_SECTION),
                        (String) values.get(METADATA_KEY),
                        ((Number) values.get(METADATA_VALUE)).doubleValue());
                    return;
                }
            } else if (values.size() == 2) {
                removeMetadata((String)values.get(METADATA_SECTION),
                    (String)values.get(METADATA_KEY));
                return;
            }
        }

        warn("ADD_METADATA object is invalid: " + arg);
    }

    private void handleClearMetadataTab(Object arg) {
        if (arg instanceof String) {
            clearMetadataTab((String)arg);
        } else {
            warn("CLEAR_METADATA_TAB object is invalid: " + arg);
        }
    }

    private void handleAppVersionChange(Object arg) {
        if (arg instanceof String) {
            updateAppVersion((String)arg);
        } else {
            warn("UPDATE_APP_VERSION object is invalid: " + arg);
        }
    }

    private void handleRemoveMetadata(Object arg) {
        if (arg instanceof List) {
            @SuppressWarnings("unchecked")
            List<String> metadata = (List<String>)arg;
            if (metadata.size() == 2) {
                removeMetadata(metadata.get(METADATA_SECTION),
                    metadata.get(METADATA_KEY));
                return;
            }
        }

        warn("REMOVE_METADATA object is invalid: " + arg);
    }

    private void handleStartSession(Object arg) {
        if (arg instanceof List) {
            @SuppressWarnings("unchecked")
            List<Object> metadata = (List<Object>)arg;
            if (metadata.size() == 4) {
                Object id = metadata.get(0);
                Object startTime = metadata.get(1);
                Object handledCount = metadata.get(2);
                Object unhandledCount = metadata.get(3);

                if (id instanceof String && startTime instanceof String
                    && handledCount instanceof Integer && unhandledCount instanceof Integer) {
                    startedSession((String)id, (String)startTime,
                        (Integer) handledCount, (Integer) unhandledCount);
                    return;
                }
            }
        }

        warn("START_SESSION object is invalid: " + arg);
    }

    private void handleStopSession() {
        stoppedSession();
    }

    private void handleReleaseStageChange(Object arg) {
        if (arg instanceof String) {
            updateReleaseStage((String)arg);
        } else {
            warn("UPDATE_RELEASE_STAGE object is invalid: " + arg);
        }
    }

    private void handleOrientationChange(Object arg) {
        if (arg instanceof Integer) {
            updateOrientation((int) arg);
        } else if (arg == null) {
            warn("UPDATE_ORIENTATION object is null");
        } else {
            warn("UPDATE_ORIENTATION object is invalid: " + arg);
        }
    }

    private void handleForegroundActivityChange(Object arg) {
        if (arg instanceof List) {
            @SuppressWarnings("unchecked")
            List<Object> metadata = (List<Object>)arg;
            if (metadata.size() == 2) {
                updateInForeground((boolean) metadata.get(0), (String) metadata.get(1));
                return;
            }
        }

        warn("UPDATE_IN_FOREGROUND object is invalid: " + arg);
    }

    private void handleUserIdChange(Object arg) {
        if (arg == null) {
            updateUserId("");
        } else if (arg instanceof String) {
            updateUserId((String)arg);
        } else {
            warn("UPDATE_USER_ID object is invalid: " + arg);
        }
    }

    private void handleUserNameChange(Object arg) {
        if (arg == null) {
            updateUserName("");
        } else if (arg instanceof String) {
            updateUserName((String)arg);
        } else {
            warn("UPDATE_USER_NAME object is invalid: " + arg);
        }
    }

    private void handleUserEmailChange(Object arg) {
        if (arg == null) {
            updateUserEmail("");
        } else if (arg instanceof String) {
            updateUserEmail((String)arg);
        } else {
            warn("UPDATE_USER_EMAIL object is invalid: " + arg);
        }
    }

    private void handleBuildUUIDChange(Object arg) {
        if (arg == null) {
            updateBuildUUID("");
        } else if (arg instanceof String) {
            updateBuildUUID((String)arg);
        } else {
            warn("UPDATE_BUILD_UUID object is invalid: " + arg);
        }
    }

    private void handleContextChange(Object arg) {
        if (arg == null) {
            updateContext("");
        } else if (arg instanceof String) {
            updateContext((String)arg);
        } else {
            warn("UPDATE_CONTEXT object is invalid: " + arg);
        }
    }

    private void handleLowMemoryChange(Object arg) {
        if (arg instanceof Boolean) {
            updateLowMemory((Boolean)arg);
        } else {
            warn("UPDATE_LOW_MEMORY object is invalid: " + arg);
        }
    }

    private void handleUpdateMetadata(Object arg) {
        if (arg instanceof MetaData) {
            updateMetadata(arg);
        } else {
            warn("UPDATE_METADATA object is invalid: " + arg);
        }
    }

    private void warn(String message) {
        if (loggingEnabled) {
            Log.w(LOG_TAG, message);
        }
    }
}
