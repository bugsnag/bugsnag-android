package com.bugsnag.android;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import java.util.HashMap;
import java.util.Map;

/**
 * Used to automatically create breadcrumbs for system events
 */
public class EventReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        try {
            Map<String, String> meta = new HashMap<>();

            if (intent.getExtras() != null) {
                for (String key : intent.getExtras().keySet()) {
                    meta.put(key, intent.getExtras().get(key).toString());
                }
            }

            String actionName = intent.getAction();
            if (actionName.contains(".")) {
                actionName = actionName.substring(actionName.lastIndexOf(".") + 1, actionName.length());
            }

            Bugsnag.leaveBreadcrumb(actionName, BreadcrumbType.LOG, meta);

        } catch (Exception ex) {
            Logger.warn("Failed to leave breadcrumb in EventReceiver: " + ex.getMessage());
        }
    }

    /**
     * Creates a new Intent filter with all the intents to record breadcrumbs for
     *
     * @return The intent filter
     */
    public static IntentFilter getIntentFilter() {

        IntentFilter filter = new IntentFilter();

        // Broadcast actions and categories can be found in text files in the android folder
        // e.g. ~/Library/Android/sdk/platforms/android-9/data/broadcast_actions.txt
        // See http://stackoverflow.com/a/27601497

        // This code adds the contents of the files from API 09, and then the diffs for the
        // following versions
        addIntentFiltersApi09(filter);
        addIntentFiltersApi10(filter);
        addIntentFiltersApi11(filter);
        addIntentFiltersApi12(filter);
        addIntentFiltersApi13(filter);
        addIntentFiltersApi14(filter);
        addIntentFiltersApi15(filter);
        addIntentFiltersApi16(filter);
        addIntentFiltersApi17(filter);
        addIntentFiltersApi18(filter);
        addIntentFiltersApi19(filter);
        addIntentFiltersApi20(filter);
        addIntentFiltersApi21(filter);
        addIntentFiltersApi22(filter);
        addIntentFiltersApi23(filter);
        addIntentFiltersApi24(filter);
        addIntentFiltersApi25(filter);

        return filter;
    }

    /**
     * Adds all the broadcast_actions and categories defined in Android API 09
     *
     * @param filter The filter to add to
     */
    private static void addIntentFiltersApi09(IntentFilter filter) {

        filter.addAction("android.app.action.ACTION_PASSWORD_CHANGED");
        filter.addAction("android.app.action.ACTION_PASSWORD_FAILED");
        filter.addAction("android.app.action.ACTION_PASSWORD_SUCCEEDED");
        filter.addAction("android.app.action.DEVICE_ADMIN_DISABLED");
        filter.addAction("android.app.action.DEVICE_ADMIN_DISABLE_REQUESTED");
        filter.addAction("android.app.action.DEVICE_ADMIN_ENABLED");
        filter.addAction("android.bluetooth.a2dp.action.SINK_STATE_CHANGED");
        filter.addAction("android.bluetooth.adapter.action.DISCOVERY_FINISHED");
        filter.addAction("android.bluetooth.adapter.action.DISCOVERY_STARTED");
        filter.addAction("android.bluetooth.adapter.action.LOCAL_NAME_CHANGED");
        filter.addAction("android.bluetooth.adapter.action.SCAN_MODE_CHANGED");
        filter.addAction("android.bluetooth.adapter.action.STATE_CHANGED");
        filter.addAction("android.bluetooth.device.action.ACL_CONNECTED");
        filter.addAction("android.bluetooth.device.action.ACL_DISCONNECTED");
        filter.addAction("android.bluetooth.device.action.ACL_DISCONNECT_REQUESTED");
        filter.addAction("android.bluetooth.device.action.BOND_STATE_CHANGED");
        filter.addAction("android.bluetooth.device.action.CLASS_CHANGED");
        filter.addAction("android.bluetooth.device.action.FOUND");
        filter.addAction("android.bluetooth.device.action.NAME_CHANGED");
        filter.addAction("android.bluetooth.devicepicker.action.DEVICE_SELECTED");
        filter.addAction("android.bluetooth.devicepicker.action.LAUNCH");
        filter.addAction("android.bluetooth.headset.action.AUDIO_STATE_CHANGED");
        filter.addAction("android.bluetooth.headset.action.STATE_CHANGED");
        filter.addAction("android.intent.action.ACTION_POWER_CONNECTED");
        filter.addAction("android.intent.action.ACTION_POWER_DISCONNECTED");
        filter.addAction("android.intent.action.ACTION_SHUTDOWN");
        filter.addAction("android.intent.action.AIRPLANE_MODE");
        //filter.addAction("android.intent.action.BATTERY_CHANGED"); - Ignore this, changes every percent
        filter.addAction("android.intent.action.BATTERY_LOW");
        filter.addAction("android.intent.action.BATTERY_OKAY");
        filter.addAction("android.intent.action.BOOT_COMPLETED");
        filter.addAction("android.intent.action.CAMERA_BUTTON");
        filter.addAction("android.intent.action.CONFIGURATION_CHANGED");
        filter.addAction("android.intent.action.DATA_SMS_RECEIVED");
        filter.addAction("android.intent.action.DATE_CHANGED");
        filter.addAction("android.intent.action.DEVICE_STORAGE_LOW");
        filter.addAction("android.intent.action.DEVICE_STORAGE_OK");
        filter.addAction("android.intent.action.DOCK_EVENT");
        filter.addAction("android.intent.action.EXTERNAL_APPLICATIONS_AVAILABLE");
        filter.addAction("android.intent.action.EXTERNAL_APPLICATIONS_UNAVAILABLE");
        filter.addAction("android.intent.action.GTALK_CONNECTED");
        filter.addAction("android.intent.action.GTALK_DISCONNECTED");
        filter.addAction("android.intent.action.HEADSET_PLUG");
        filter.addAction("android.intent.action.INPUT_METHOD_CHANGED");
        filter.addAction("android.intent.action.LOCALE_CHANGED");
        filter.addAction("android.intent.action.MANAGE_PACKAGE_STORAGE");
        filter.addAction("android.intent.action.MEDIA_BAD_REMOVAL");
        filter.addAction("android.intent.action.MEDIA_BUTTON");
        filter.addAction("android.intent.action.MEDIA_CHECKING");
        filter.addAction("android.intent.action.MEDIA_EJECT");
        filter.addAction("android.intent.action.MEDIA_MOUNTED");
        filter.addAction("android.intent.action.MEDIA_NOFS");
        filter.addAction("android.intent.action.MEDIA_REMOVED");
        filter.addAction("android.intent.action.MEDIA_SCANNER_FINISHED");
        filter.addAction("android.intent.action.MEDIA_SCANNER_SCAN_FILE");
        filter.addAction("android.intent.action.MEDIA_SCANNER_STARTED");
        filter.addAction("android.intent.action.MEDIA_SHARED");
        filter.addAction("android.intent.action.MEDIA_UNMOUNTABLE");
        filter.addAction("android.intent.action.MEDIA_UNMOUNTED");
        filter.addAction("android.intent.action.NEW_OUTGOING_CALL");
        filter.addAction("android.intent.action.PACKAGE_ADDED");
        filter.addAction("android.intent.action.PACKAGE_CHANGED");
        filter.addAction("android.intent.action.PACKAGE_DATA_CLEARED");
        filter.addAction("android.intent.action.PACKAGE_INSTALL");
        filter.addAction("android.intent.action.PACKAGE_REMOVED");
        filter.addAction("android.intent.action.PACKAGE_REPLACED");
        filter.addAction("android.intent.action.PACKAGE_RESTARTED");
        filter.addAction("android.intent.action.PHONE_STATE");
        filter.addAction("android.intent.action.PROVIDER_CHANGED");
        filter.addAction("android.intent.action.REBOOT");
        filter.addAction("android.intent.action.SCREEN_OFF");
        filter.addAction("android.intent.action.SCREEN_ON");
        filter.addAction("android.intent.action.TIMEZONE_CHANGED");
        filter.addAction("android.intent.action.TIME_SET");
        //filter.addAction("android.intent.action.TIME_TICK"); - Ignore this, adds a message every minute
        filter.addAction("android.intent.action.UID_REMOVED");
        filter.addAction("android.intent.action.UMS_CONNECTED");
        filter.addAction("android.intent.action.UMS_DISCONNECTED");
        filter.addAction("android.intent.action.USER_PRESENT");
        filter.addAction("android.intent.action.WALLPAPER_CHANGED");
        filter.addAction("android.media.AUDIO_BECOMING_NOISY");
        filter.addAction("android.media.RINGER_MODE_CHANGED");
        filter.addAction("android.media.SCO_AUDIO_STATE_CHANGED");
        filter.addAction("android.media.VIBRATE_SETTING_CHANGED");
        filter.addAction("android.media.action.CLOSE_AUDIO_EFFECT_CONTROL_SESSION");
        filter.addAction("android.media.action.OPEN_AUDIO_EFFECT_CONTROL_SESSION");
        filter.addAction("android.net.conn.BACKGROUND_DATA_SETTING_CHANGED");
        filter.addAction("android.net.wifi.NETWORK_IDS_CHANGED");
        filter.addAction("android.net.wifi.RSSI_CHANGED");
        filter.addAction("android.net.wifi.SCAN_RESULTS");
        filter.addAction("android.net.wifi.STATE_CHANGE");
        filter.addAction("android.net.wifi.WIFI_STATE_CHANGED");
        filter.addAction("android.net.wifi.supplicant.CONNECTION_CHANGE");
        filter.addAction("android.net.wifi.supplicant.STATE_CHANGE");
        filter.addAction("android.provider.Telephony.SIM_FULL");
        filter.addAction("android.provider.Telephony.SMS_RECEIVED");
        filter.addAction("android.provider.Telephony.SMS_REJECTED");
        filter.addAction("android.provider.Telephony.WAP_PUSH_RECEIVED");
        filter.addAction("android.speech.tts.TTS_QUEUE_PROCESSING_COMPLETED");
        filter.addAction("android.speech.tts.engine.TTS_DATA_INSTALLED");

        filter.addCategory("android.intent.category.ALTERNATIVE");
        filter.addCategory("android.intent.category.BROWSABLE");
        filter.addCategory("android.intent.category.CAR_DOCK");
        filter.addCategory("android.intent.category.CAR_MODE");
        filter.addCategory("android.intent.category.DEFAULT");
        filter.addCategory("android.intent.category.DESK_DOCK");
        filter.addCategory("android.intent.category.DEVELOPMENT_PREFERENCE");
        filter.addCategory("android.intent.category.EMBED");
        filter.addCategory("android.intent.category.HOME");
        filter.addCategory("android.intent.category.INFO");
        filter.addCategory("android.intent.category.LAUNCHER");
        filter.addCategory("android.intent.category.MONKEY");
        filter.addCategory("android.intent.category.OPENABLE");
        filter.addCategory("android.intent.category.PREFERENCE");
        filter.addCategory("android.intent.category.SELECTED_ALTERNATIVE");
        filter.addCategory("android.intent.category.TAB");
    }

    /**
     * Adds diffs in the broadcast_actions and categories defined in Android API 10
     *
     * @param filter The filter to add to
     */
    private static void addIntentFiltersApi10(IntentFilter filter) {
        // No diffs for the broadcast actions

        // No diffs for the categories
    }

    /**
     * Adds diffs in the broadcast_actions and categories defined in Android API 11
     *
     * @param filter The filter to add to
     */
    private static void addIntentFiltersApi11(IntentFilter filter) {

        filter.addAction("android.app.action.ACTION_PASSWORD_EXPIRING");
        filter.addAction("android.bluetooth.a2dp.profile.action.CONNECTION_STATE_CHANGED");
        filter.addAction("android.bluetooth.a2dp.profile.action.PLAYING_STATE_CHANGED");
        filter.addAction("android.bluetooth.adapter.action.CONNECTION_STATE_CHANGED");
        filter.addAction("android.bluetooth.headset.action.VENDOR_SPECIFIC_HEADSET_EVENT");
        filter.addAction("android.bluetooth.headset.profile.action.AUDIO_STATE_CHANGED");
        filter.addAction("android.bluetooth.headset.profile.action.CONNECTION_STATE_CHANGED");
        filter.addAction("android.bluetooth.inputdevice.action.INPUT_DEVICE_STATE_CHANGED");
        filter.addAction("android.bluetooth.pan.action.STATE_CHANGED");
        filter.addAction("android.intent.action.PROXY_CHANGE");

        filter.addCategory("android.intent.category.APP_MARKET");
        filter.addCategory("android.intent.category.HE_DESK_DOCK");
        filter.addCategory("android.intent.category.LE_DESK_DOCK");
    }

    /**
     * Adds diffs in the broadcast_actions and categories defined in Android API 12
     *
     * @param filter The filter to add to
     */
    private static void addIntentFiltersApi12(IntentFilter filter) {

        filter.addAction("android.bluetooth.input.profile.action.CONNECTION_STATE_CHANGED");
        filter.addAction("android.bluetooth.pan.profile.action.CONNECTION_STATE_CHANGED");
        filter.addAction("android.intent.action.MY_PACKAGE_REPLACED");
        filter.addAction("android.intent.action.PACKAGE_FIRST_LAUNCH");

        // No diffs for the categories
    }

    /**
     * Adds diffs in the broadcast_actions and categories defined in Android API 13
     *
     * @param filter The filter to add to
     */
    private static void addIntentFiltersApi13(IntentFilter filter) {
        // No diffs for the broadcast actions

        // No diffs for the categories
    }

    /**
     * Adds diffs in the broadcast_actions and categories defined in Android API 14
     *
     * @param filter The filter to add to
     */
    private static void addIntentFiltersApi14(IntentFilter filter) {

        filter.addAction("android.hardware.action.NEW_PICTURE");
        filter.addAction("android.hardware.action.NEW_VIDEO");
        filter.addAction("android.intent.action.FETCH_VOICEMAIL");
        filter.addAction("android.intent.action.NEW_VOICEMAIL");
        filter.addAction("android.intent.action.PACKAGE_FULLY_REMOVED");
        filter.addAction("android.intent.action.PACKAGE_NEEDS_VERIFICATION");
        filter.addAction("android.media.ACTION_SCO_AUDIO_STATE_UPDATED");
        filter.addAction("android.net.wifi.p2p.CONNECTION_STATE_CHANGE");
        filter.addAction("android.net.wifi.p2p.PEERS_CHANGED");
        filter.addAction("android.net.wifi.p2p.STATE_CHANGED");
        filter.addAction("android.net.wifi.p2p.THIS_DEVICE_CHANGED");
        filter.addAction("android.provider.Telephony.SMS_CB_RECEIVED");
        filter.addAction("android.provider.Telephony.SMS_EMERGENCY_CB_RECEIVED");

        // No diffs for the categories
    }

    /**
     * Adds diffs in the broadcast_actions and categories defined in Android API 15
     *
     * @param filter The filter to add to
     */
    private static void addIntentFiltersApi15(IntentFilter filter) {

        filter.addAction("android.bluetooth.device.action.UUID");

        filter.addCategory("android.intent.category.APP_BROWSER");
        filter.addCategory("android.intent.category.APP_CALCULATOR");
        filter.addCategory("android.intent.category.APP_CALENDAR");
        filter.addCategory("android.intent.category.APP_CONTACTS");
        filter.addCategory("android.intent.category.APP_EMAIL");
        filter.addCategory("android.intent.category.APP_GALLERY");
        filter.addCategory("android.intent.category.APP_MAPS");
        filter.addCategory("android.intent.category.APP_MESSAGING");
        filter.addCategory("android.intent.category.APP_MUSIC");
    }

    /**
     * Adds diffs in the broadcast_actions and categories defined in Android API 16
     *
     * @param filter The filter to add to
     */
    private static void addIntentFiltersApi16(IntentFilter filter) {

        filter.addAction("android.hardware.input.action.QUERY_KEYBOARD_LAYOUTS");
        filter.addAction("android.net.nsd.STATE_CHANGED");
        filter.addAction("android.net.wifi.p2p.DISCOVERY_STATE_CHANGE");
        filter.addAction("android.provider.Telephony.SMS_SERVICE_CATEGORY_PROGRAM_DATA_RECEIVED");

        // No diffs for the categories
    }

    /**
     * Adds diffs in the broadcast_actions and categories defined in Android API 17
     *
     * @param filter The filter to add to
     */
    private static void addIntentFiltersApi17(IntentFilter filter) {

        filter.addAction("android.intent.action.DREAMING_STARTED");
        filter.addAction("android.intent.action.DREAMING_STOPPED");
        filter.addAction("android.intent.action.PACKAGE_VERIFIED");

        // No diffs for the categories
    }

    /**
     * Adds diffs in the broadcast_actions and categories defined in Android API 18
     *
     * @param filter The filter to add to
     */
    private static void addIntentFiltersApi18(IntentFilter filter) {

        filter.addAction("android.net.conn.CONNECTIVITY_CHANGE");
        filter.addAction("android.nfc.action.ADAPTER_STATE_CHANGED");

        // No diffs for the categories
    }

    /**
     * Adds diffs in the broadcast_actions and categories defined in Android API 19
     *
     * @param filter The filter to add to
     */
    private static void addIntentFiltersApi19(IntentFilter filter) {

        filter.addAction("android.bluetooth.device.action.PAIRING_REQUEST");
        filter.addAction("android.intent.action.CONTENT_CHANGED");
        filter.addAction("android.intent.action.DATA_SMS_RECEIVED");
        filter.addAction("android.intent.action.DOWNLOAD_COMPLETE");
        filter.addAction("android.intent.action.DOWNLOAD_NOTIFICATION_CLICKED");
        filter.addAction("android.provider.Telephony.SIM_FULL");
        filter.addAction("android.provider.Telephony.SMS_CB_RECEIVED");
        filter.addAction("android.provider.Telephony.SMS_DELIVER");
        filter.addAction("android.provider.Telephony.SMS_EMERGENCY_CB_RECEIVED");
        filter.addAction("android.provider.Telephony.SMS_RECEIVED");
        filter.addAction("android.provider.Telephony.SMS_REJECTED");
        filter.addAction("android.provider.Telephony.SMS_SERVICE_CATEGORY_PROGRAM_DATA_RECEIVED");
        filter.addAction("android.provider.Telephony.WAP_PUSH_DELIVER");
        filter.addAction("android.provider.Telephony.WAP_PUSH_RECEIVED");

        // No diffs for the categories
    }

    /**
     * Adds diffs in the broadcast_actions and categories defined in Android API 20
     *
     * @param filter The filter to add to
     */
    private static void addIntentFiltersApi20(IntentFilter filter) {
        // No diffs for the broadcast actions

        // No diffs for the categories
    }

    /**
     * Adds diffs in the broadcast_actions and categories defined in Android API 21
     *
     * @param filter The filter to add to
     */
    private static void addIntentFiltersApi21(IntentFilter filter) {

        filter.addAction("android.app.action.LOCK_TASK_ENTERING");
        filter.addAction("android.app.action.LOCK_TASK_EXITING");
        filter.addAction("android.app.action.NEXT_ALARM_CLOCK_CHANGED");
        filter.addAction("android.app.action.PROFILE_PROVISIONING_COMPLETE");
        filter.addAction("android.hardware.hdmi.action.OSD_MESSAGE");
        filter.addAction("android.intent.action.APPLICATION_RESTRICTIONS_CHANGED");
        filter.addAction("android.intent.action.HEADSET_PLUG");
        filter.addAction("android.media.action.HDMI_AUDIO_PLUG");
        filter.addAction("android.net.scoring.SCORER_CHANGED");
        filter.addAction("android.net.scoring.SCORE_NETWORKS");
        filter.addAction("android.os.action.POWER_SAVE_MODE_CHANGED");

        filter.addCategory("android.intent.category.LEANBACK_LAUNCHER");
        filter.addCategory("android.intent.category.NOTIFICATION_PREFERENCES");
    }

    /**
     * Adds diffs in the broadcast_actions and categories defined in Android API 22
     *
     * @param filter The filter to add to
     */
    private static void addIntentFiltersApi22(IntentFilter filter) {
        // No diffs for the broadcast actions

        // No diffs for the categories
    }

    /**
     * Adds diffs in the broadcast_actions and categories defined in Android API 23
     *
     * @param filter The filter to add to
     */
    private static void addIntentFiltersApi23(IntentFilter filter) {

        filter.addAction("android.app.action.DEVICE_OWNER_CHANGED");
        filter.addAction("android.app.action.INTERRUPTION_FILTER_CHANGED");
        filter.addAction("android.app.action.NOTIFICATION_POLICY_ACCESS_GRANTED_CHANGED");
        filter.addAction("android.app.action.NOTIFICATION_POLICY_CHANGED");
        filter.addAction("android.app.action.SYSTEM_UPDATE_POLICY_CHANGED");
        filter.addAction("android.os.action.DEVICE_IDLE_MODE_CHANGED");

        filter.addCategory("android.intent.category.USAGE_ACCESS_CONFIG");
        filter.addCategory("android.intent.category.VOICE");
    }

    /**
     * Adds diffs in the broadcast_actions and categories defined in Android API 24
     *
     * @param filter The filter to add to
     */
    private static void addIntentFiltersApi24(IntentFilter filter) {

        filter.addAction("android.intent.action.LOCKED_BOOT_COMPLETED");
        filter.addAction("android.intent.action.PACKAGES_SUSPENDED");
        filter.addAction("android.intent.action.PACKAGES_UNSUSPENDED");
        filter.addAction("android.intent.action.USER_UNLOCKED");
        filter.addAction("android.net.conn.RESTRICT_BACKGROUND_CHANGED");
        filter.addAction("android.provider.action.DEFAULT_SMS_PACKAGE_CHANGED");
        filter.addAction("android.provider.action.EXTERNAL_PROVIDER_CHANGE");
        filter.addAction("android.provider.action.SYNC_VOICEMAIL");

        filter.addCategory("android.service.quicksettings.action.QS_TILE_PREFERENCES");
    }

    /**
     * Adds diffs in the broadcast_actions and categories defined in Android API 25
     *
     * @param filter The filter to add to
     */
    private static void addIntentFiltersApi25(IntentFilter filter) {
        // No diffs for the broadcast actions

        // No diffs for the categories
    }
}
