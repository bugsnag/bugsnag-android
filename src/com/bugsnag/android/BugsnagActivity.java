/*
    Bugsnag Notifier for Android
    Copyright (c) 2012 Bugsnag
    http://www.bugsnag.com

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

        http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
*/

package com.bugsnag.android;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;

import java.lang.ref.WeakReference;
import java.util.LinkedList;
import java.util.List;

/**
 * BugsnagActivity keeps track of the top-most and all currently active
 * activities, so you can use this information when debugging.
 *
 * The BugsnagActivity class should be used as a parent class of each android
 * Activity in your application. If you cannot inherit from BugsnagActivity
 * you may instead call the static activity lifecycle methods provided by the
 * inner helper class.
 */
public class BugsnagActivity extends Activity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Helper.onActivityCreate(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Helper.onActivityResume(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        Helper.onActivityPause();
    }


    public static class Helper {
        private static List<WeakReference<Context>> storedContexts = new LinkedList<WeakReference<Context>>();

        public static void onActivityCreate(Context context) {
            // Store a weak reference to this context
            storedContexts.add(new WeakReference<Context>(context));
        }

        public static void onActivityResume(Context context) {
            // Set that we are the current "top-most" activity
            setCurrentActivity(context);

            // Clear any dead references from the list of storedContexts
            // This gives us an approximation of the current Activity stack
            List<WeakReference<Context>> toRemove = new LinkedList<WeakReference<Context>>();
            List<String> goodContexts = new LinkedList<String>();
            for (WeakReference<Context> ref : storedContexts) {
                if (ref.get() == null) {
                    toRemove.add(ref);
                } else {
                    goodContexts.add(getActivityName(ref.get()));
                }
            }
            storedContexts.removeAll(toRemove);
            Bugsnag.setActivityStack(goodContexts);
        }

        public static void onActivityPause() {
            clearCurrentActivity();
        }

        private static void setCurrentActivity(Context context) {
            Bugsnag.setContext(getActivityName(context));
        }

        private static void clearCurrentActivity() {
            Bugsnag.setContext(null);
        }

        private static String getActivityName(Context context) {
            String name = context.getClass().getName();
            return name.substring(name.lastIndexOf('.') + 1);
        }
    }
}