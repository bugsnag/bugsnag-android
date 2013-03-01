package com.bugsnag.android;

import java.lang.ref.WeakReference;
import java.util.LinkedList;
import java.util.List;

import android.app.Activity;
import android.content.Context;

public class ActivityStack {
    private static List<WeakReference<Context>> storedContexts = new LinkedList<WeakReference<Context>>();
    private static WeakReference<Context> topContext;

    public static void add(Activity activity) {
        prune();
        storedContexts.add(new WeakReference<Context>(activity));
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
    }

    public static Context getTopActivity() {
        if(topContext != null) {
            return topContext.get();
        } else {
            return null;
        }
    }

    public static void clearTopActivity() {
        topContext = null;
    }

    public static String getTopActivityName() {
        String name = null;
        Context context = getTopActivity();
        if(context != null) {
            name = getContextName(context);
        }
        return name;
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

    public static String getContextName(Context context) {
        String name = context.getClass().getName();
        return name.substring(name.lastIndexOf('.') + 1);
    }
}