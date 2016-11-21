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
        Logger.warn("GOT EVENT IN RECEIVER");

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
    }

    /**
     * Creates a new Intent filter with all the intents to record breadcrumbs for
     * @return The intent filter
     */
    public static IntentFilter getIntentFilter() {

        IntentFilter i = new IntentFilter();

        // See https://developer.android.com/reference/android/content/Intent.html
        // Standard Activity Actions
        i.addAction(Intent.ACTION_MAIN);
        i.addAction(Intent.ACTION_VIEW);
        i.addAction(Intent.ACTION_ATTACH_DATA);
        i.addAction(Intent.ACTION_EDIT);
        i.addAction(Intent.ACTION_PICK);
        i.addAction(Intent.ACTION_CHOOSER);
        i.addAction(Intent.ACTION_GET_CONTENT);
        i.addAction(Intent.ACTION_DIAL);
        i.addAction(Intent.ACTION_CALL);
        i.addAction(Intent.ACTION_SEND);
        i.addAction(Intent.ACTION_SENDTO);
        i.addAction(Intent.ACTION_ANSWER);
        i.addAction(Intent.ACTION_INSERT);
        i.addAction(Intent.ACTION_DELETE);
        i.addAction(Intent.ACTION_RUN);
        i.addAction(Intent.ACTION_SYNC);
        i.addAction(Intent.ACTION_PICK_ACTIVITY);
        i.addAction(Intent.ACTION_SEARCH);
        i.addAction(Intent.ACTION_WEB_SEARCH);
        i.addAction(Intent.ACTION_FACTORY_TEST);

        // Standard Broadcast Actions
//        i.addAction(Intent.ACTION_TIME_TICK); This one ticks every 1 minute
        i.addAction(Intent.ACTION_TIME_CHANGED);
        i.addAction(Intent.ACTION_TIMEZONE_CHANGED);
        i.addAction(Intent.ACTION_BOOT_COMPLETED);
        i.addAction(Intent.ACTION_PACKAGE_ADDED);
        i.addAction(Intent.ACTION_PACKAGE_CHANGED);
        i.addAction(Intent.ACTION_PACKAGE_REMOVED);
        i.addAction(Intent.ACTION_PACKAGE_RESTARTED);
        i.addAction(Intent.ACTION_PACKAGE_DATA_CLEARED);
//        i.addAction(Intent.ACTION_PACKAGES_SUSPENDED); Deprecated?
//        i.addAction(Intent.ACTION_PACKAGES_UNSUSPENDED);
        i.addAction(Intent.ACTION_UID_REMOVED);
        i.addAction(Intent.ACTION_BATTERY_CHANGED);
        i.addAction(Intent.ACTION_POWER_CONNECTED);
        i.addAction(Intent.ACTION_POWER_DISCONNECTED);
        i.addAction(Intent.ACTION_SHUTDOWN);

        // Standard Categories
        i.addCategory(Intent.CATEGORY_DEFAULT);
        i.addCategory(Intent.CATEGORY_BROWSABLE);
        i.addCategory(Intent.CATEGORY_TAB);
        i.addCategory(Intent.CATEGORY_ALTERNATIVE);
        i.addCategory(Intent.CATEGORY_SELECTED_ALTERNATIVE);
        i.addCategory(Intent.CATEGORY_LAUNCHER);
        i.addCategory(Intent.CATEGORY_INFO);
        i.addCategory(Intent.CATEGORY_HOME);
        i.addCategory(Intent.CATEGORY_PREFERENCE);
        i.addCategory(Intent.CATEGORY_TEST);
//        i.addCategory(Intent.CATEGORY_CAR_DOCK); Deprecated?
//        i.addCategory(Intent.CATEGORY_DESK_DOCK);
//        i.addCategory(Intent.CATEGORY_LE_DESK_DOCK);
//        i.addCategory(Intent.CATEGORY_HE_DESK_DOCK);
//        i.addCategory(Intent.CATEGORY_CAR_MODE);
//        i.addCategory(Intent.CATEGORY_APP_MARKET);

        // From http://stackoverflow.com/a/27601497
        i.addAction("android.app.action.ACTION_PASSWORD_CHANGED");
        i.addAction("android.app.action.ACTION_PASSWORD_EXPIRING");
        i.addAction("android.app.action.ACTION_PASSWORD_FAILED");
        i.addAction("android.app.action.ACTION_PASSWORD_SUCCEEDED");
        i.addAction("android.app.action.DEVICE_ADMIN_DISABLED");
        i.addAction("android.app.action.DEVICE_ADMIN_DISABLE_REQUESTED");
        i.addAction("android.app.action.DEVICE_ADMIN_ENABLED");
        i.addAction("android.app.action.LOCK_TASK_ENTERING");
        i.addAction("android.app.action.LOCK_TASK_EXITING");
        i.addAction("android.app.action.NEXT_ALARM_CLOCK_CHANGED");
        i.addAction("android.app.action.PROFILE_PROVISIONING_COMPLETE");
        i.addAction("android.bluetooth.a2dp.profile.action.CONNECTION_STATE_CHANGED");
        i.addAction("android.bluetooth.a2dp.profile.action.PLAYING_STATE_CHANGED");
        i.addAction("android.bluetooth.adapter.action.CONNECTION_STATE_CHANGED");
        i.addAction("android.bluetooth.adapter.action.DISCOVERY_FINISHED");
        i.addAction("android.bluetooth.adapter.action.DISCOVERY_STARTED");
        i.addAction("android.bluetooth.adapter.action.LOCAL_NAME_CHANGED");
        i.addAction("android.bluetooth.adapter.action.SCAN_MODE_CHANGED");
        i.addAction("android.bluetooth.adapter.action.STATE_CHANGED");
        i.addAction("android.bluetooth.device.action.ACL_CONNECTED");
        i.addAction("android.bluetooth.device.action.ACL_DISCONNECTED");
        i.addAction("android.bluetooth.device.action.ACL_DISCONNECT_REQUESTED");
        i.addAction("android.bluetooth.device.action.BOND_STATE_CHANGED");
        i.addAction("android.bluetooth.device.action.CLASS_CHANGED");
        i.addAction("android.bluetooth.device.action.FOUND");
        i.addAction("android.bluetooth.device.action.NAME_CHANGED");
        i.addAction("android.bluetooth.device.action.PAIRING_REQUEST");
        i.addAction("android.bluetooth.device.action.UUID");
        i.addAction("android.bluetooth.devicepicker.action.DEVICE_SELECTED");
        i.addAction("android.bluetooth.devicepicker.action.LAUNCH");
        i.addAction("android.bluetooth.headset.action.VENDOR_SPECIFIC_HEADSET_EVENT");
        i.addAction("android.bluetooth.headset.profile.action.AUDIO_STATE_CHANGED");
        i.addAction("android.bluetooth.headset.profile.action.CONNECTION_STATE_CHANGED");
        i.addAction("android.bluetooth.input.profile.action.CONNECTION_STATE_CHANGED");
        i.addAction("android.bluetooth.pan.profile.action.CONNECTION_STATE_CHANGED");
        i.addAction("android.hardware.action.NEW_PICTURE");
        i.addAction("android.hardware.action.NEW_VIDEO");
        i.addAction("android.hardware.hdmi.action.OSD_MESSAGE");
        i.addAction("android.hardware.input.action.QUERY_KEYBOARD_LAYOUTS");
        i.addAction("android.intent.action.ACTION_POWER_CONNECTED");
        i.addAction("android.intent.action.ACTION_POWER_DISCONNECTED");
        i.addAction("android.intent.action.ACTION_SHUTDOWN");
        i.addAction("android.intent.action.AIRPLANE_MODE");
        i.addAction("android.intent.action.APPLICATION_RESTRICTIONS_CHANGED");
        i.addAction("android.intent.action.BATTERY_CHANGED");
        i.addAction("android.intent.action.BATTERY_LOW android.intent.action.BATTERY_OKAY");
        i.addAction("android.intent.action.BOOT_COMPLETED");
        i.addAction("android.intent.action.CAMERA_BUTTON");
        i.addAction("android.intent.action.CONFIGURATION_CHANGED");
        i.addAction("android.intent.action.CONTENT_CHANGED");
        i.addAction("android.intent.action.DATA_SMS_RECEIVED");
        i.addAction("android.intent.action.DATE_CHANGED");
        i.addAction("android.intent.action.DEVICE_STORAGE_LOW");
        i.addAction("android.intent.action.DEVICE_STORAGE_OK");
        i.addAction("android.intent.action.DOCK_EVENT");
        i.addAction("android.intent.action.DOWNLOAD_COMPLETE");
        i.addAction("android.intent.action.DOWNLOAD_NOTIFICATION_CLICKED");
        i.addAction("android.intent.action.DREAMING_STARTED");
        i.addAction("android.intent.action.DREAMING_STOPPED");
        i.addAction("android.intent.action.EXTERNAL_APPLICATIONS_AVAILABLE");
        i.addAction("android.intent.action.EXTERNAL_APPLICATIONS_UNAVAILABLE");
        i.addAction("android.intent.action.FETCH_VOICEMAIL");
        i.addAction("android.intent.action.GTALK_CONNECTED");
        i.addAction("android.intent.action.GTALK_DISCONNECTED");
        i.addAction("android.intent.action.HEADSET_PLUG");
        i.addAction("android.intent.action.HEADSET_PLUG");
        i.addAction("android.intent.action.INPUT_METHOD_CHANGED");
        i.addAction("android.intent.action.LOCALE_CHANGED");
        i.addAction("android.intent.action.MANAGE_PACKAGE_STORAGE");
        i.addAction("android.intent.action.MEDIA_BAD_REMOVAL");
        i.addAction("android.intent.action.MEDIA_BUTTON");
        i.addAction("android.intent.action.MEDIA_CHECKING");
        i.addAction("android.intent.action.MEDIA_EJECT");
        i.addAction("android.intent.action.MEDIA_MOUNTED android.intent.action.MEDIA_NOFS");
        i.addAction("android.intent.action.MEDIA_REMOVED");
        i.addAction("android.intent.action.MEDIA_SCANNER_FINISHED");
        i.addAction("android.intent.action.MEDIA_SCANNER_SCAN_FILE");
        i.addAction("android.intent.action.MEDIA_SCANNER_STARTED");
        i.addAction("android.intent.action.MEDIA_SHARED");
        i.addAction("android.intent.action.MEDIA_UNMOUNTABLE");
        i.addAction("android.intent.action.MEDIA_UNMOUNTED");
        i.addAction("android.intent.action.MY_PACKAGE_REPLACED");
        i.addAction("android.intent.action.NEW_OUTGOING_CALL");
        i.addAction("android.intent.action.NEW_VOICEMAIL");
        i.addAction("android.intent.action.PACKAGE_ADDED");
        i.addAction("android.intent.action.PACKAGE_CHANGED");
        i.addAction("android.intent.action.PACKAGE_DATA_CLEARED");
        i.addAction("android.intent.action.PACKAGE_FIRST_LAUNCH");
        i.addAction("android.intent.action.PACKAGE_FULLY_REMOVED");
        i.addAction("android.intent.action.PACKAGE_INSTALL");
        i.addAction("android.intent.action.PACKAGE_NEEDS_VERIFICATION");
        i.addAction("android.intent.action.PACKAGE_REMOVED");
        i.addAction("android.intent.action.PACKAGE_REPLACED");
        i.addAction("android.intent.action.PACKAGE_RESTARTED");
        i.addAction("android.intent.action.PACKAGE_VERIFIED");
        i.addAction("android.intent.action.PHONE_STATE");
        i.addAction("android.intent.action.PROVIDER_CHANGED");
        i.addAction("android.intent.action.PROXY_CHANGE android.intent.action.REBOOT");
        i.addAction("android.intent.action.SCREEN_OFF android.intent.action.SCREEN_ON");
        i.addAction("android.intent.action.TIMEZONE_CHANGED");
        i.addAction("android.intent.action.TIME_SET android.intent.action.TIME_TICK");
        i.addAction("android.intent.action.UID_REMOVED android.intent.action.USER_PRESENT");
        i.addAction("android.intent.action.WALLPAPER_CHANGED");
        i.addAction("android.media.ACTION_SCO_AUDIO_STATE_UPDATED");
        i.addAction("android.media.AUDIO_BECOMING_NOISY android.media.RINGER_MODE_CHANGED");
        i.addAction("android.media.SCO_AUDIO_STATE_CHANGED");
        i.addAction("android.media.VIBRATE_SETTING_CHANGED");
        i.addAction("android.media.action.CLOSE_AUDIO_EFFECT_CONTROL_SESSION");
        i.addAction("android.media.action.HDMI_AUDIO_PLUG");
        i.addAction("android.media.action.OPEN_AUDIO_EFFECT_CONTROL_SESSION");
        i.addAction("android.net.conn.BACKGROUND_DATA_SETTING_CHANGED");
        i.addAction("android.net.conn.CONNECTIVITY_CHANGE android.net.nsd.STATE_CHANGED");
        i.addAction("android.net.scoring.SCORER_CHANGED");
        i.addAction("android.net.scoring.SCORE_NETWORKS");
        i.addAction("android.net.wifi.NETWORK_IDS_CHANGED android.net.wifi.RSSI_CHANGED");
        i.addAction("android.net.wifi.SCAN_RESULTS android.net.wifi.STATE_CHANGE");
        i.addAction("android.net.wifi.WIFI_STATE_CHANGED");
        i.addAction("android.net.wifi.p2p.CONNECTION_STATE_CHANGE");
        i.addAction("android.net.wifi.p2p.DISCOVERY_STATE_CHANGE");
        i.addAction("android.net.wifi.p2p.PEERS_CHANGED");
        i.addAction("android.net.wifi.p2p.STATE_CHANGED");
        i.addAction("android.net.wifi.p2p.THIS_DEVICE_CHANGED");
        i.addAction("android.net.wifi.supplicant.CONNECTION_CHANGE");
        i.addAction("android.net.wifi.supplicant.STATE_CHANGE");
        i.addAction("android.nfc.action.ADAPTER_STATE_CHANGED");
        i.addAction("android.os.action.POWER_SAVE_MODE_CHANGED");
        i.addAction("android.provider.Telephony.SIM_FULL");
        i.addAction("android.provider.Telephony.SMS_CB_RECEIVED");
        i.addAction("android.provider.Telephony.SMS_DELIVER");
        i.addAction("android.provider.Telephony.SMS_EMERGENCY_CB_RECEIVED");
        i.addAction("android.provider.Telephony.SMS_RECEIVED");
        i.addAction("android.provider.Telephony.SMS_REJECTED");
        i.addAction("android.provider.Telephony.SMS_SERVICE_CATEGORY_PROGRAM_DATA_RECEIVED");
        i.addAction("android.provider.Telephony.WAP_PUSH_DELIVER");
        i.addAction("android.provider.Telephony.WAP_PUSH_RECEIVED");
        i.addAction("android.speech.tts.TTS_QUEUE_PROCESSING_COMPLETED");
        i.addAction("android.speech.tts.engine.TTS_DATA_INSTALLED");


        return i;
    }
}
