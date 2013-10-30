package com.bugsnag.android;

import java.lang.ref.WeakReference;
import java.util.LinkedList;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.os.SystemClock;

public class ActivityStack {
    private static List<WeakReference<Context>> storedContexts = new LinkedList<WeakReference<Context>>();
    private static WeakReference<Context> topContext;
    private static long sessionStartTime = 0;
    private static long lastScreenClosed = 0;
    private static boolean inForeground = false;

    // This is how long we have to be inactive for, for us
    // to consider the session ended
    private static final long TIME_FOR_SESSION_TO_HALT = 10000;

    public static void add(Activity activity) {
        prune();
        if(!contains(activity)) storedContexts.add(new WeakReference<Context>(activity));
    }

    // TODO:JS Remove duplicate contexts
    public static List<String> getNames() {
        prune();

        List<String> goodContexts = new LinkedList<String>();
        for(WeakReference<Context> ref : storedContexts){
            if(ref.get() != null){
                goodContexts.add(getContextName(ref.get()));
            }
        }
        return goodContexts;
    }

    public static void setTopActivity(Activity activity) {
        topContext = new WeakReference<Context>(activity);
        inForeground = true;

        if(lastScreenClosed + TIME_FOR_SESSION_TO_HALT < SystemClock.elapsedRealtime()) {
            sessionStartTime = SystemClock.elapsedRealtime();
        }
    }

    public static void clearTopActivity() {
        topContext = null;
        inForeground = false;
        lastScreenClosed = SystemClock.elapsedRealtime();
    }

    public static String getTopActivityName() {
        String name = null;
        Context context = getTopActivity();
        if(context != null) {
            name = getContextName(context);
        }
        return name;
    }

    public static String getContextName(Context context) {
        String name = context.getClass().getName();
        return name.substring(name.lastIndexOf('.') + 1);
    }

    public static boolean inForeground() {
        return inForeground;
    }

    public static Long sessionLength() {
        if(inForeground) {
            return SystemClock.elapsedRealtime() - sessionStartTime;
        } else {
            return null;
        }
    }

    private static Boolean contains(Context context) {
        for(WeakReference<Context> ref : storedContexts){
            if(ref.get() == context){
                return true;
            }
        }
        return false;
    }

    private static Context getTopActivity() {
        if(topContext != null) {
            return topContext.get();
        } else {
            return null;
        }
    }

    private static void prune() {
        List<WeakReference<Context>> toRemove = new LinkedList<WeakReference<Context>>();
        for(WeakReference<Context> ref : storedContexts){
            if(ref.get() == null){
                toRemove.add(ref);
            }
        }
        
        for(WeakReference<Context> ref : toRemove) {
            storedContexts.remove(ref);
        }
    }
}
