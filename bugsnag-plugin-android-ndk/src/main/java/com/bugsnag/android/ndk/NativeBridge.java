package com.bugsnag.android.ndk;

import com.bugsnag.android.Breadcrumb;
import com.bugsnag.android.Configuration;
import com.bugsnag.android.MetaData;
import com.bugsnag.android.NativeInterface;

import android.os.Build;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.File;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

    private static final int METADATA_SECTION = 0;
    private static final int METADATA_KEY = 1;
    private static final int METADATA_VALUE = 2;
    private static final String LOG_TAG = "BugsnagNDK:NativeBridge";
    private static final Lock lock = new ReentrantLock();
    private static final AtomicBoolean installed = new AtomicBoolean(false);

    public static native void install(@NonNull String reportingDirectory, boolean autoNotify,
                                      int apiLevel, boolean is32bit);

    public static native void enableCrashReporting();

    public static native void disableCrashReporting();

    public static native void deliverReportAtPath(@NonNull String filePath);

    public static native void addBreadcrumb(@NonNull String name, @NonNull String type,
                                            @NonNull String timestamp, @NonNull Object metadata);

    public static native void addMetadataString(@NonNull String tab, @NonNull String key,
                                                @NonNull String value);

    public static native void addMetadataDouble(@NonNull String tab, @NonNull String key,
                                                double value);

    public static native void addMetadataBoolean(@NonNull String tab, @NonNull String key,
                                                 boolean value);

    public static native void addHandledEvent();

    public static native void addUnhandledEvent();

    public static native void clearBreadcrumbs();

    public static native void clearMetadataTab(@NonNull String tab);

    public static native void removeMetadata(@NonNull String tab,@NonNull  String key);

    public static native void startedSession(@NonNull String sessionID, @NonNull String key,
                                             int handledCount, int unhandledCount);

    public static native void stoppedSession();

    public static native void updateAppVersion(@NonNull String appVersion);

    public static native void updateBuildUUID(@NonNull String uuid);

    public static native void updateContext(@NonNull String context);

    public static native void updateInForeground(boolean inForeground,
                                                 @NonNull String activityName);

    public static native void updateLowMemory(boolean lowMemory);

    public static native void updateOrientation(int orientation);

    public static native void updateMetadata(@NonNull Object metadata);

    public static native void updateReleaseStage(@NonNull String releaseStage);

    public static native void updateUserId(@NonNull String newValue);

    public static native void updateUserEmail(@NonNull String newValue);

    public static native void updateUserName(@NonNull String newValue);

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
    public void update(@NonNull Observable observable, @Nullable Object rawMessage) {
        NativeInterface.Message message = parseMessage(rawMessage);
        if (message == null) {
            return;
        }
        Object arg = message.value;

        switch (message.type) {
            case INSTALL:
                handleInstallMessage(arg);
                break;
            case ENABLE_NATIVE_CRASH_REPORTING:
                enableCrashReporting();
                break;
            case DISABLE_NATIVE_CRASH_REPORTING:
                disableCrashReporting();
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
            case UPDATE_NOTIFY_RELEASE_STAGES:
                handleNotifyReleaseStagesChange(arg);
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
    private NativeInterface.Message parseMessage(@Nullable Object rawMessage) {
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

    private void handleInstallMessage(@NonNull Object arg) {
        lock.lock();
        try {
            if (installed.get()) {
                warn("Received duplicate setup message with arg: " + arg);
            } else if (arg instanceof List) {
                @SuppressWarnings("unchecked")
                List<Object> values = (List<Object>)arg;
                if (values.size() > 0 && values.get(0) instanceof Configuration) {
                    Configuration config = (Configuration)values.get(0);
                    String reportPath = reportDirectory + "/" + UUID.randomUUID().toString() + ".crash";
                    install(reportPath, config.getDetectNdkCrashes(), Build.VERSION.SDK_INT,
                        is32bit());
                    installed.set(true);
                }
            } else {
                warn("Received install message with incorrect arg: " + arg);
            }
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
            Map<String, String> metadata = new HashMap<>();
            if (crumb.getMetadata() != null) {
                for (Map.Entry<String, String> entry : crumb.getMetadata().entrySet()) {
                    if (entry.getValue() != null) {
                        metadata.put(makeSafe(entry.getKey()), makeSafe(entry.getValue()));
                    }
                }
            }

            addBreadcrumb(crumb.getName(), crumb.getType().toString(),
                crumb.getTimestamp(), metadata);
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
                String section = makeSafe((String)values.get(METADATA_SECTION));
                String key = makeSafe((String)values.get(METADATA_KEY));

                if (values.get(METADATA_VALUE) instanceof String) {
                    addMetadataString(section, key,
                        makeSafe((String) values.get(METADATA_VALUE)));
                    return;
                } else if (values.get(METADATA_VALUE) instanceof Boolean) {
                    addMetadataBoolean(section, key,
                        (Boolean) values.get(METADATA_VALUE));
                    return;
                } else if (values.get(METADATA_VALUE) instanceof Number) {
                    addMetadataDouble(section, key,
                        ((Number) values.get(METADATA_VALUE)).doubleValue());
                    return;
                }
            } else if (values.size() == 2 && values.get(METADATA_SECTION) instanceof String
                    && values.get(METADATA_KEY) instanceof String) {
                String section = makeSafe((String)values.get(METADATA_SECTION));
                String key = makeSafe((String)values.get(METADATA_KEY));
                removeMetadata(section, key);
                return;
            }
        }

        warn("ADD_METADATA object is invalid: " + arg);
    }

    private void handleClearMetadataTab(Object arg) {
        if (arg instanceof String) {
            clearMetadataTab(makeSafe((String)arg));
        } else {
            warn("CLEAR_METADATA_TAB object is invalid: " + arg);
        }
    }

    private void handleAppVersionChange(Object arg) {
        if (arg instanceof String) {
            updateAppVersion(makeSafe((String)arg));
        } else {
            warn("UPDATE_APP_VERSION object is invalid: " + arg);
        }
    }

    private void handleRemoveMetadata(Object arg) {
        if (arg instanceof List) {
            @SuppressWarnings("unchecked")
            List<String> metadata = (List<String>)arg;
            if (metadata.size() == 2) {
                removeMetadata(makeSafe(metadata.get(METADATA_SECTION)),
                    makeSafe(metadata.get(METADATA_KEY)));
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
        if (arg instanceof Configuration) {
            Configuration config = (Configuration) arg;
            String releaseStage = config.getReleaseStage();
            updateReleaseStage(makeSafe(releaseStage));
            enableOrDisableReportingIfNeeded(config);
        } else {
            warn("UPDATE_RELEASE_STAGE object is invalid: " + arg);
        }
    }

    private void handleNotifyReleaseStagesChange(Object arg) {
        if (arg instanceof Configuration) {
            enableOrDisableReportingIfNeeded((Configuration) arg);
        } else {
            warn("UPDATE_NOTIFY_RELEASE_STAGES object is invalid: " + arg);
        }
    }

    private void enableOrDisableReportingIfNeeded(Configuration config) {
        if (config.shouldNotifyForReleaseStage(config.getReleaseStage())) {
            if (config.getDetectNdkCrashes()) {
                enableCrashReporting();
            }
            // TODO enable ANRs in future when supported in Unity
        } else {
            disableCrashReporting();
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
                updateInForeground((boolean) metadata.get(0), makeSafe((String) metadata.get(1)));
                return;
            }
        }

        warn("UPDATE_IN_FOREGROUND object is invalid: " + arg);
    }

    private void handleUserIdChange(Object arg) {
        if (arg == null) {
            updateUserId("");
        } else if (arg instanceof String) {
            updateUserId(makeSafe((String)arg));
        } else {
            warn("UPDATE_USER_ID object is invalid: " + arg);
        }
    }

    private void handleUserNameChange(Object arg) {
        if (arg == null) {
            updateUserName("");
        } else if (arg instanceof String) {
            updateUserName(makeSafe((String)arg));
        } else {
            warn("UPDATE_USER_NAME object is invalid: " + arg);
        }
    }

    private void handleUserEmailChange(Object arg) {
        if (arg == null) {
            updateUserEmail("");
        } else if (arg instanceof String) {
            updateUserEmail(makeSafe((String)arg));
        } else {
            warn("UPDATE_USER_EMAIL object is invalid: " + arg);
        }
    }

    private void handleBuildUUIDChange(Object arg) {
        if (arg == null) {
            updateBuildUUID("");
        } else if (arg instanceof String) {
            updateBuildUUID(makeSafe((String)arg));
        } else {
            warn("UPDATE_BUILD_UUID object is invalid: " + arg);
        }
    }

    private void handleContextChange(Object arg) {
        if (arg == null) {
            updateContext("");
        } else if (arg instanceof String) {
            updateContext(makeSafe((String)arg));
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
        if (arg instanceof Map) {
            updateMetadata(arg);
        } else {
            warn("UPDATE_METADATA object is invalid: " + arg);
        }
    }

    /**
     * Ensure the string is safe to be passed to native layer by forcing the encoding
     * to UTF-8.
     */
    private String makeSafe(String text) {
        // The Android platform default charset is always UTF-8
        return text == null
                ? null
                : new String(text.getBytes(Charset.defaultCharset()));
    }

    private void warn(String message) {
        if (loggingEnabled) {
            Log.w(LOG_TAG, message);
        }
    }
}
