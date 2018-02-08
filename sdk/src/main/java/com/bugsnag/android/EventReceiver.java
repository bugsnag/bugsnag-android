package com.bugsnag.android;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.annotation.NonNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Used to automatically create breadcrumbs for system events
 * Broadcast actions and categories can be found in text files in the android folder
 * e.g. ~/Library/Android/sdk/platforms/android-9/data/broadcast_actions.txt
 * See http://stackoverflow.com/a/27601497
 */
public class EventReceiver extends BroadcastReceiver {

    private static final String INTENT_ACTION_KEY = "Intent Action";

    @NonNull
    private static final Map<String, BreadcrumbType> actions = buildActions();
    @NonNull
    private static final List<String> categories = buildCategories();

    private final Client client;

    public EventReceiver(Client client) {
        this.client = client;
    }

    @Override
    public void onReceive(Context context, @NonNull Intent intent) {
        try {
            Map<String, String> meta = new HashMap<>();
            String fullAction = intent.getAction();
            String shortAction = shortenActionNameIfNeeded(intent.getAction());
            meta.put(INTENT_ACTION_KEY, fullAction); // always add the Intent Action

            if (intent.getExtras() != null) {
                for (String key : intent.getExtras().keySet()) {
                    String val = intent.getExtras().get(key).toString();

                    if (isAndroidKey(key)) { // shorten the Intent action
                        meta.put("Extra", String.format("%s: %s", shortAction, val));
                    } else {
                        meta.put(key, val);
                    }
                }
            }

            BreadcrumbType type =
                actions.containsKey(fullAction) ? actions.get(fullAction) : BreadcrumbType.LOG;

            if (client.getConfig().isAutomaticallyCollectingBreadcrumbs()) {
                client.leaveBreadcrumb(shortAction, type, meta);
            }

        } catch (Exception ex) {
            Logger.warn("Failed to leave breadcrumb in EventReceiver: " + ex.getMessage());
        }
    }

    static boolean isAndroidKey(@NonNull String actionName) {
        return actionName.startsWith("android.");
    }

    @NonNull
    static String shortenActionNameIfNeeded(@NonNull String action) {
        if (isAndroidKey(action)) {
            return action.substring(action.lastIndexOf(".") + 1, action.length());
        } else {
            return action;
        }
    }

    /**
     * Builds a map of intent actions and their breadcrumb type.
     *
     * Noisy breadcrumbs are commented out, along with anything that involves a state change.
     * @return the action map
     */
    @NonNull
    private static Map<String, BreadcrumbType> buildActions() {
        HashMap<String, BreadcrumbType> actions = new HashMap<>();

        // api 9
        actions.put("android.app.action.ACTION_PASSWORD_CHANGED", BreadcrumbType.LOG);
        actions.put("android.app.action.ACTION_PASSWORD_FAILED", BreadcrumbType.LOG);
        actions.put("android.app.action.ACTION_PASSWORD_SUCCEEDED", BreadcrumbType.LOG);
        actions.put("android.app.action.DEVICE_ADMIN_DISABLED", BreadcrumbType.USER);
        actions.put("android.app.action.DEVICE_ADMIN_DISABLE_REQUESTED", BreadcrumbType.USER);
        actions.put("android.app.action.DEVICE_ADMIN_ENABLED", BreadcrumbType.USER);
        //actions.put("android.bluetooth.a2dp.action.SINK_STATE_CHANGED", BreadcrumbType.LOG);
        actions.put("android.bluetooth.adapter.action.DISCOVERY_FINISHED", BreadcrumbType.LOG);
        actions.put("android.bluetooth.adapter.action.DISCOVERY_STARTED", BreadcrumbType.LOG);
        actions.put("android.bluetooth.adapter.action.LOCAL_NAME_CHANGED", BreadcrumbType.LOG);
        actions.put("android.bluetooth.adapter.action.SCAN_MODE_CHANGED", BreadcrumbType.LOG);
        //actions.put("android.bluetooth.adapter.action.STATE_CHANGED", BreadcrumbType.LOG);
        actions.put("android.bluetooth.device.action.ACL_CONNECTED", BreadcrumbType.LOG);
        actions.put("android.bluetooth.device.action.ACL_DISCONNECTED", BreadcrumbType.LOG);
        actions.put("android.bluetooth.device.action.ACL_DISCONNECT_REQUESTED", BreadcrumbType.LOG);
        //actions.put("android.bluetooth.device.action.BOND_STATE_CHANGED", BreadcrumbType.LOG);
        actions.put("android.bluetooth.device.action.CLASS_CHANGED", BreadcrumbType.LOG);
        actions.put("android.bluetooth.device.action.FOUND", BreadcrumbType.LOG);
        actions.put("android.bluetooth.device.action.NAME_CHANGED", BreadcrumbType.LOG);
        actions.put("android.bluetooth.devicepicker.action.DEVICE_SELECTED", BreadcrumbType.LOG);
        actions.put("android.bluetooth.devicepicker.action.LAUNCH", BreadcrumbType.LOG);
        actions.put("android.bluetooth.headset.action.AUDIO_STATE_CHANGED", BreadcrumbType.LOG);
        //actions.put("android.bluetooth.headset.action.STATE_CHANGED", BreadcrumbType.LOG);
        actions.put("android.intent.action.ACTION_POWER_CONNECTED", BreadcrumbType.USER);
        actions.put("android.intent.action.ACTION_POWER_DISCONNECTED", BreadcrumbType.USER);
        actions.put("android.intent.action.ACTION_SHUTDOWN", BreadcrumbType.USER);
        actions.put("android.intent.action.AIRPLANE_MODE", BreadcrumbType.USER);

        // Ignore this, changes every percent
        //actions.put("android.intent.action.BATTERY_CHANGED", BreadcrumbType.LOG);
        actions.put("android.intent.action.BATTERY_LOW", BreadcrumbType.LOG);
        actions.put("android.intent.action.BATTERY_OKAY", BreadcrumbType.LOG);
        actions.put("android.intent.action.BOOT_COMPLETED", BreadcrumbType.LOG);
        actions.put("android.intent.action.CAMERA_BUTTON", BreadcrumbType.USER);
        actions.put("android.intent.action.CONFIGURATION_CHANGED", BreadcrumbType.LOG);
        actions.put("android.intent.action.DATA_SMS_RECEIVED", BreadcrumbType.LOG);
        actions.put("android.intent.action.DATE_CHANGED", BreadcrumbType.LOG);
        actions.put("android.intent.action.DEVICE_STORAGE_LOW", BreadcrumbType.LOG);
        actions.put("android.intent.action.DEVICE_STORAGE_OK", BreadcrumbType.LOG);
        actions.put("android.intent.action.DOCK_EVENT", BreadcrumbType.USER);
        actions.put("android.intent.action.EXTERNAL_APPLICATIONS_AVAILABLE", BreadcrumbType.LOG);
        actions.put("android.intent.action.EXTERNAL_APPLICATIONS_UNAVAILABLE", BreadcrumbType.LOG);
        actions.put("android.intent.action.GTALK_CONNECTED", BreadcrumbType.LOG);
        actions.put("android.intent.action.GTALK_DISCONNECTED", BreadcrumbType.LOG);
        actions.put("android.intent.action.HEADSET_PLUG", BreadcrumbType.USER);
        actions.put("android.intent.action.INPUT_METHOD_CHANGED", BreadcrumbType.LOG);
        actions.put("android.intent.action.LOCALE_CHANGED", BreadcrumbType.LOG);
        actions.put("android.intent.action.MANAGE_PACKAGE_STORAGE", BreadcrumbType.LOG);
        actions.put("android.intent.action.MEDIA_BAD_REMOVAL", BreadcrumbType.LOG);
        actions.put("android.intent.action.MEDIA_BUTTON", BreadcrumbType.USER);
        actions.put("android.intent.action.MEDIA_CHECKING", BreadcrumbType.LOG);
        actions.put("android.intent.action.MEDIA_EJECT", BreadcrumbType.USER);
        actions.put("android.intent.action.MEDIA_MOUNTED", BreadcrumbType.LOG);
        actions.put("android.intent.action.MEDIA_NOFS", BreadcrumbType.LOG);
        actions.put("android.intent.action.MEDIA_REMOVED", BreadcrumbType.USER);
        actions.put("android.intent.action.MEDIA_SCANNER_FINISHED", BreadcrumbType.PROCESS);
        actions.put("android.intent.action.MEDIA_SCANNER_SCAN_FILE", BreadcrumbType.PROCESS);
        actions.put("android.intent.action.MEDIA_SCANNER_STARTED", BreadcrumbType.PROCESS);
        actions.put("android.intent.action.MEDIA_SHARED", BreadcrumbType.LOG);
        actions.put("android.intent.action.MEDIA_UNMOUNTABLE", BreadcrumbType.LOG);
        actions.put("android.intent.action.MEDIA_UNMOUNTED", BreadcrumbType.LOG);
        actions.put("android.intent.action.NEW_OUTGOING_CALL", BreadcrumbType.LOG);
        actions.put("android.intent.action.PACKAGE_ADDED", BreadcrumbType.LOG);
        actions.put("android.intent.action.PACKAGE_CHANGED", BreadcrumbType.LOG);
        actions.put("android.intent.action.PACKAGE_DATA_CLEARED", BreadcrumbType.LOG);
        actions.put("android.intent.action.PACKAGE_INSTALL", BreadcrumbType.LOG);
        actions.put("android.intent.action.PACKAGE_REMOVED", BreadcrumbType.LOG);
        actions.put("android.intent.action.PACKAGE_REPLACED", BreadcrumbType.LOG);
        actions.put("android.intent.action.PACKAGE_RESTARTED", BreadcrumbType.NAVIGATION);
        actions.put("android.intent.action.PHONE_STATE", BreadcrumbType.LOG);
        actions.put("android.intent.action.PROVIDER_CHANGED", BreadcrumbType.LOG);
        actions.put("android.intent.action.REBOOT", BreadcrumbType.LOG);
        actions.put("android.intent.action.SCREEN_OFF", BreadcrumbType.USER);
        actions.put("android.intent.action.SCREEN_ON", BreadcrumbType.USER);
        actions.put("android.intent.action.TIMEZONE_CHANGED", BreadcrumbType.LOG);
        actions.put("android.intent.action.TIME_SET", BreadcrumbType.LOG);
        // Ignore this, adds a message every minute
        //actions.put("android.intent.action.TIME_TICK", BreadcrumbType.LOG);
        actions.put("android.intent.action.UID_REMOVED", BreadcrumbType.LOG);
        actions.put("android.intent.action.UMS_CONNECTED", BreadcrumbType.LOG);
        actions.put("android.intent.action.UMS_DISCONNECTED", BreadcrumbType.LOG);
        actions.put("android.intent.action.USER_PRESENT", BreadcrumbType.USER);
        actions.put("android.intent.action.WALLPAPER_CHANGED", BreadcrumbType.LOG);
        actions.put("android.media.AUDIO_BECOMING_NOISY", BreadcrumbType.LOG);
        actions.put("android.media.RINGER_MODE_CHANGED", BreadcrumbType.LOG);
        //actions.put("android.media.SCO_AUDIO_STATE_CHANGED", BreadcrumbType.LOG);
        actions.put("android.media.VIBRATE_SETTING_CHANGED", BreadcrumbType.LOG);
        actions.put("android.media.action.CLOSE_AUDIO_EFFECT_CONTROL_SESSION", BreadcrumbType.LOG);
        actions.put("android.media.action.OPEN_AUDIO_EFFECT_CONTROL_SESSION", BreadcrumbType.LOG);
        actions.put("android.net.conn.BACKGROUND_DATA_SETTING_CHANGED", BreadcrumbType.LOG);
        actions.put("android.net.wifi.NETWORK_IDS_CHANGED", BreadcrumbType.LOG);
        actions.put("android.net.wifi.RSSI_CHANGED", BreadcrumbType.LOG);
        //actions.put("android.net.wifi.SCAN_RESULTS", BreadcrumbType.LOG);
        //actions.put("android.net.wifi.STATE_CHANGE", BreadcrumbType.LOG);
        //actions.put("android.net.wifi.WIFI_STATE_CHANGED", BreadcrumbType.LOG);
        actions.put("android.net.wifi.supplicant.CONNECTION_CHANGE", BreadcrumbType.LOG);
        //actions.put("android.net.wifi.supplicant.STATE_CHANGE", BreadcrumbType.LOG);
        actions.put("android.provider.Telephony.SIM_FULL", BreadcrumbType.LOG);
        actions.put("android.provider.Telephony.SMS_RECEIVED", BreadcrumbType.LOG);
        actions.put("android.provider.Telephony.SMS_REJECTED", BreadcrumbType.LOG);
        actions.put("android.provider.Telephony.WAP_PUSH_RECEIVED", BreadcrumbType.LOG);
        actions.put("android.speech.tts.TTS_QUEUE_PROCESSING_COMPLETED", BreadcrumbType.LOG);
        actions.put("android.speech.tts.engine.TTS_DATA_INSTALLED", BreadcrumbType.LOG);

        // api 11
        actions.put("android.app.action.ACTION_PASSWORD_EXPIRING", BreadcrumbType.LOG);
        //actions.put("android.bluetooth.a2dp.profile.action.CONNECTION_STATE_CHANGED",
        // BreadcrumbType.LOG);
        actions.put("android.bluetooth.a2dp.profile.action.PLAYING_STATE_CHANGED",
            BreadcrumbType.LOG);
        //actions.put("android.bluetooth.adapter.action.CONNECTION_STATE_CHANGED",
        // BreadcrumbType.LOG);
        actions.put("android.bluetooth.headset.action.VENDOR_SPECIFIC_HEADSET_EVENT",
            BreadcrumbType.LOG);
        //actions.put("android.bluetooth.headset.profile.action.AUDIO_STATE_CHANGED",
        // BreadcrumbType.LOG);
        //actions.put("android.bluetooth.headset.profile.action.CONNECTION_STATE_CHANGED",
        // BreadcrumbType.LOG);
        //actions.put("android.bluetooth.inputdevice.action.INPUT_DEVICE_STATE_CHANGED",
        // BreadcrumbType.LOG);
        //actions.put("android.bluetooth.pan.action.STATE_CHANGED", BreadcrumbType.LOG);
        actions.put("android.intent.action.PROXY_CHANGE", BreadcrumbType.LOG);

        // api 12
        //actions.put("android.bluetooth.input.profile.action.CONNECTION_STATE_CHANGED",
        // BreadcrumbType.LOG);
        //actions.put("android.bluetooth.pan.profile.action.CONNECTION_STATE_CHANGED",
        // BreadcrumbType.LOG);
        actions.put("android.intent.action.MY_PACKAGE_REPLACED", BreadcrumbType.LOG);
        actions.put("android.intent.action.PACKAGE_FIRST_LAUNCH", BreadcrumbType.NAVIGATION);

        // api 14
        actions.put("android.hardware.action.NEW_PICTURE", BreadcrumbType.LOG);
        actions.put("android.hardware.action.NEW_VIDEO", BreadcrumbType.LOG);
        actions.put("android.intent.action.FETCH_VOICEMAIL", BreadcrumbType.LOG);
        actions.put("android.intent.action.NEW_VOICEMAIL", BreadcrumbType.LOG);
        actions.put("android.intent.action.PACKAGE_FULLY_REMOVED", BreadcrumbType.LOG);
        actions.put("android.intent.action.PACKAGE_NEEDS_VERIFICATION", BreadcrumbType.LOG);
        actions.put("android.media.ACTION_SCO_AUDIO_STATE_UPDATED", BreadcrumbType.LOG);
        //actions.put("android.net.wifi.p2p.CONNECTION_STATE_CHANGE", BreadcrumbType.LOG);
        actions.put("android.net.wifi.p2p.PEERS_CHANGED", BreadcrumbType.LOG);
        actions.put("android.net.wifi.p2p.STATE_CHANGED", BreadcrumbType.LOG);
        actions.put("android.net.wifi.p2p.THIS_DEVICE_CHANGED", BreadcrumbType.LOG);
        actions.put("android.provider.Telephony.SMS_CB_RECEIVED", BreadcrumbType.LOG);
        actions.put("android.provider.Telephony.SMS_EMERGENCY_CB_RECEIVED", BreadcrumbType.LOG);

        // api 15
        actions.put("android.bluetooth.device.action.UUID", BreadcrumbType.LOG);

        // api 16
        actions.put("android.hardware.input.action.QUERY_KEYBOARD_LAYOUTS", BreadcrumbType.LOG);
        //actions.put("android.net.nsd.STATE_CHANGED", BreadcrumbType.LOG);
        //actions.put("android.net.wifi.p2p.DISCOVERY_STATE_CHANGE", BreadcrumbType.LOG);
        actions.put("android.provider.Telephony.SMS_SERVICE_CATEGORY_PROGRAM_DATA_RECEIVED",
            BreadcrumbType.LOG);

        // api 17
        actions.put("android.intent.action.DREAMING_STARTED", BreadcrumbType.NAVIGATION);
        actions.put("android.intent.action.DREAMING_STOPPED", BreadcrumbType.NAVIGATION);
        actions.put("android.intent.action.PACKAGE_VERIFIED", BreadcrumbType.LOG);

        // api 18
        actions.put("android.net.conn.CONNECTIVITY_CHANGE", BreadcrumbType.LOG);
        //actions.put("android.nfc.action.ADAPTER_STATE_CHANGED", BreadcrumbType.LOG);

        // api 19
        actions.put("android.bluetooth.device.action.PAIRING_REQUEST", BreadcrumbType.LOG);
        actions.put("android.intent.action.CONTENT_CHANGED", BreadcrumbType.LOG);
        actions.put("android.intent.action.DATA_SMS_RECEIVED", BreadcrumbType.LOG);
        actions.put("android.intent.action.DOWNLOAD_COMPLETE", BreadcrumbType.LOG);
        actions.put("android.intent.action.DOWNLOAD_NOTIFICATION_CLICKED", BreadcrumbType.USER);
        actions.put("android.provider.Telephony.SIM_FULL", BreadcrumbType.LOG);
        actions.put("android.provider.Telephony.SMS_CB_RECEIVED", BreadcrumbType.LOG);
        actions.put("android.provider.Telephony.SMS_DELIVER", BreadcrumbType.LOG);
        actions.put("android.provider.Telephony.SMS_EMERGENCY_CB_RECEIVED", BreadcrumbType.LOG);
        actions.put("android.provider.Telephony.SMS_RECEIVED", BreadcrumbType.LOG);
        actions.put("android.provider.Telephony.SMS_REJECTED", BreadcrumbType.LOG);
        actions.put("android.provider.Telephony.SMS_SERVICE_CATEGORY_PROGRAM_DATA_RECEIVED",
            BreadcrumbType.LOG);
        actions.put("android.provider.Telephony.WAP_PUSH_DELIVER", BreadcrumbType.LOG);
        actions.put("android.provider.Telephony.WAP_PUSH_RECEIVED", BreadcrumbType.LOG);

        // api 21
        actions.put("android.app.action.LOCK_TASK_ENTERING", BreadcrumbType.NAVIGATION);
        actions.put("android.app.action.LOCK_TASK_EXITING", BreadcrumbType.NAVIGATION);
        actions.put("android.app.action.NEXT_ALARM_CLOCK_CHANGED", BreadcrumbType.LOG);
        actions.put("android.app.action.PROFILE_PROVISIONING_COMPLETE", BreadcrumbType.LOG);
        actions.put("android.hardware.hdmi.action.OSD_MESSAGE", BreadcrumbType.LOG);
        actions.put("android.intent.action.APPLICATION_RESTRICTIONS_CHANGED", BreadcrumbType.LOG);
        actions.put("android.intent.action.HEADSET_PLUG", BreadcrumbType.USER);
        actions.put("android.media.action.HDMI_AUDIO_PLUG", BreadcrumbType.USER);
        actions.put("android.net.scoring.SCORER_CHANGED", BreadcrumbType.LOG);
        actions.put("android.net.scoring.SCORE_NETWORKS", BreadcrumbType.LOG);
        actions.put("android.os.action.POWER_SAVE_MODE_CHANGED", BreadcrumbType.LOG);

        // api 23
        actions.put("android.app.action.DEVICE_OWNER_CHANGED", BreadcrumbType.LOG);
        actions.put("android.app.action.INTERRUPTION_FILTER_CHANGED", BreadcrumbType.LOG);
        actions.put("android.app.action.NOTIFICATION_POLICY_ACCESS_GRANTED_CHANGED",
            BreadcrumbType.LOG);
        actions.put("android.app.action.NOTIFICATION_POLICY_CHANGED", BreadcrumbType.LOG);
        actions.put("android.app.action.SYSTEM_UPDATE_POLICY_CHANGED", BreadcrumbType.LOG);
        actions.put("android.os.action.DEVICE_IDLE_MODE_CHANGED", BreadcrumbType.LOG);

        // api 24
        actions.put("android.intent.action.LOCKED_BOOT_COMPLETED", BreadcrumbType.LOG);
        actions.put("android.intent.action.PACKAGES_SUSPENDED", BreadcrumbType.LOG);
        actions.put("android.intent.action.PACKAGES_UNSUSPENDED", BreadcrumbType.LOG);
        actions.put("android.intent.action.USER_UNLOCKED", BreadcrumbType.USER);
        actions.put("android.net.conn.RESTRICT_BACKGROUND_CHANGED", BreadcrumbType.LOG);
        actions.put("android.provider.action.DEFAULT_SMS_PACKAGE_CHANGED", BreadcrumbType.LOG);
        actions.put("android.provider.action.EXTERNAL_PROVIDER_CHANGE", BreadcrumbType.LOG);
        actions.put("android.provider.action.SYNC_VOICEMAIL", BreadcrumbType.LOG);

        // api 26
        actions.put("android.accounts.LOGIN_ACCOUNTS_CHANGED", BreadcrumbType.LOG);
        actions.put("android.accounts.action.ACCOUNT_REMOVED", BreadcrumbType.LOG);
        actions.put("android.app.action.APPLICATION_DELEGATION_SCOPES_CHANGED", BreadcrumbType.LOG);
        actions.put("android.appwidget.action.APPWIDGET_DELETED", BreadcrumbType.LOG);
        actions.put("android.appwidget.action.APPWIDGET_DISABLED", BreadcrumbType.LOG);
        actions.put("android.appwidget.action.APPWIDGET_ENABLED", BreadcrumbType.LOG);
        actions.put("android.appwidget.action.APPWIDGET_HOST_RESTORED", BreadcrumbType.LOG);
        actions.put("android.appwidget.action.APPWIDGET_RESTORED", BreadcrumbType.LOG);
        actions.put("android.appwidget.action.APPWIDGET_UPDATE", BreadcrumbType.LOG);
        actions.put("android.appwidget.action.APPWIDGET_UPDATE_OPTIONS", BreadcrumbType.LOG);
        //actions.put("android.bluetooth.inputhost.profile.action.CONNECTION_STATE_CHANGED",
        //BreadcrumbType.LOG);
        actions.put("android.content.pm.action.SESSION_COMMITTED", BreadcrumbType.LOG);
        actions.put("android.hardware.usb.action.USB_ACCESSORY_ATTACHED", BreadcrumbType.USER);
        actions.put("android.hardware.usb.action.USB_ACCESSORY_DETACHED", BreadcrumbType.USER);
        actions.put("android.hardware.usb.action.USB_DEVICE_ATTACHED", BreadcrumbType.USER);
        actions.put("android.hardware.usb.action.USB_DEVICE_DETACHED", BreadcrumbType.USER);
        actions.put("android.intent.action.CLOSE_SYSTEM_DIALOGS", BreadcrumbType.LOG);
        actions.put("android.intent.action.DROPBOX_ENTRY_ADDED", BreadcrumbType.LOG);
        actions.put("android.intent.action.UMS_CONNECTED", BreadcrumbType.LOG);
        actions.put("android.intent.action.UMS_DISCONNECTED", BreadcrumbType.LOG);
        actions.put("android.media.tv.action.INITIALIZE_PROGRAMS", BreadcrumbType.LOG);
        actions.put("android.media.tv.action.PREVIEW_PROGRAM_ADDED_TO_WATCH_NEXT",
            BreadcrumbType.LOG);
        actions.put("android.media.tv.action.PREVIEW_PROGRAM_BROWSABLE_DISABLED",
            BreadcrumbType.LOG);
        actions.put("android.media.tv.action.WATCH_NEXT_PROGRAM_BROWSABLE_DISABLED",
            BreadcrumbType.LOG);
        //actions.put("android.net.wifi.aware.action.WIFI_AWARE_STATE_CHANGED",
        // BreadcrumbType.LOG);
        actions.put("android.security.STORAGE_CHANGED", BreadcrumbType.LOG);
        actions.put("android.security.action.KEYCHAIN_CHANGED", BreadcrumbType.LOG);
        actions.put("android.security.action.KEY_ACCESS_CHANGED", BreadcrumbType.LOG);
        actions.put("android.security.action.TRUST_STORE_CHANGED", BreadcrumbType.LOG);
        actions.put("android.telephony.action.DEFAULT_SMS_SUBSCRIPTION_CHANGED",
            BreadcrumbType.LOG);
        actions.put("android.telephony.action.DEFAULT_SUBSCRIPTION_CHANGED", BreadcrumbType.LOG);
        return actions;
    }

    @NonNull
    private static List<String> buildCategories() {
        List<String> categories = new ArrayList<>();

        // api 9
        categories.add("android.intent.category.ALTERNATIVE");
        categories.add("android.intent.category.BROWSABLE");
        categories.add("android.intent.category.CAR_DOCK");
        categories.add("android.intent.category.CAR_MODE");
        categories.add("android.intent.category.DEFAULT");
        categories.add("android.intent.category.DESK_DOCK");
        categories.add("android.intent.category.DEVELOPMENT_PREFERENCE");
        categories.add("android.intent.category.EMBED");
        categories.add("android.intent.category.HOME");
        categories.add("android.intent.category.INFO");
        categories.add("android.intent.category.LAUNCHER");
        categories.add("android.intent.category.MONKEY");
        categories.add("android.intent.category.OPENABLE");
        categories.add("android.intent.category.PREFERENCE");
        categories.add("android.intent.category.SELECTED_ALTERNATIVE");
        categories.add("android.intent.category.TAB");

        // api 11
        categories.add("android.intent.category.APP_MARKET");
        categories.add("android.intent.category.HE_DESK_DOCK");
        categories.add("android.intent.category.LE_DESK_DOCK");

        // api 15

        categories.add("android.intent.category.APP_BROWSER");
        categories.add("android.intent.category.APP_CALCULATOR");
        categories.add("android.intent.category.APP_CALENDAR");
        categories.add("android.intent.category.APP_CONTACTS");
        categories.add("android.intent.category.APP_EMAIL");
        categories.add("android.intent.category.APP_GALLERY");
        categories.add("android.intent.category.APP_MAPS");
        categories.add("android.intent.category.APP_MESSAGING");
        categories.add("android.intent.category.APP_MUSIC");

        // api 21
        categories.add("android.intent.category.LEANBACK_LAUNCHER");
        categories.add("android.intent.category.NOTIFICATION_PREFERENCES");

        // api 23
        categories.add("android.intent.category.USAGE_ACCESS_CONFIG");
        categories.add("android.intent.category.VOICE");

        // api 24
        categories.add("android.service.quicksettings.action.QS_TILE_PREFERENCES");

        return categories;
    }

    /**
     * Creates a new Intent filter with all the intents to record breadcrumbs for
     *
     * @return The intent filter
     */
    @NonNull
    public static IntentFilter getIntentFilter() {
        IntentFilter filter = new IntentFilter();

        for (String action : actions.keySet()) {
            filter.addAction(action);
        }

        for (String category : categories) {
            filter.addCategory(category);
        }

        return filter;
    }

}
