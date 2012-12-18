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

import java.lang.ref.WeakReference;
import java.util.LinkedList;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;

/**
 * BugsnagActivity keeps track of the top-most and all currently active
 * activities, so you can use this information when debugging.
 * 
 * The BugsnagActivity class should be used as a parent class of each android
 * Activity in your application.
 */
public class BugsnagActivity extends Activity {
    @Override
    protected void onResume() {
        super.onResume();

        // Set that we are the current "top-most" activity
        Bugsnag.setContext(this);
        Bugsnag.addActivity(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        
        String context = null;
        Bugsnag.setContext(context);
    }
}