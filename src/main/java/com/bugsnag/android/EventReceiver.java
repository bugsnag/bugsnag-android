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
        i.addAction(Intent.ACTION_BATTERY_LOW);
        i.addAction(Intent.ACTION_BATTERY_OKAY);


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


        return i;
    }
}
