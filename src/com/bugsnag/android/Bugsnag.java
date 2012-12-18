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

import java.net.HttpURLConnection;
import java.net.URL;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.lang.ref.WeakReference;
import java.lang.Thread.UncaughtExceptionHandler;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import org.json.JSONObject;
import org.json.JSONArray;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.PackageInfo;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.text.TextUtils;
import android.util.Log;
import android.util.Xml;

/**
 * The Bugsnag class is used to capture exception and/or notify bugsnag.com.
 * <p>
 * For example:
 * <p>
 * <pre>
 * Bugsnag.register(this, "your-api-key-here");
 * </pre>
 */
public class Bugsnag {
    static final String LOG_TAG = "Bugsnag";

    // Constants
    private static final String PREFS_NAME = "Bugsnag";
    private static final String DEFAULT_ENDPOINT = "notify.bugsnag.com";
    private static String NOTIFIER_NAME = "Android Bugsnag Notifier";
    private static final String NOTIFIER_VERSION = "1.0.0";
    private static final String NOTIFIER_URL = "http://www.bugsnag.com";
    private static final String UNSENT_EXCEPTION_PATH = "/unsent_bugsnag_exceptions/";

    // Properties
    private static String apiKey;
    private static String context;
    private static String userId;
    private static String releaseStage = "production";
    private static String[] notifyReleaseStages = new String[]{"production"};
    private static boolean autoNotify = true;
    private static Map<String, Object> metaData;
    private static boolean useSSL = false;
    private static String endpoint = DEFAULT_ENDPOINT;

    // Other private vars
    private static String packageName = "unknown";
    private static String appVersion = "unknown";
    private static String osVersion = android.os.Build.VERSION.RELEASE;
    private static List<WeakReference<Context>> activityStack = new LinkedList<WeakReference<Context>>();
    private static String filePath;
    private static boolean diskStorageEnabled = false;

    static {
        metaData = new HashMap<String, Object>();
        Bugsnag.addToTab("device", "OS Version", android.os.Build.VERSION.RELEASE);
        Bugsnag.addToTab("device", "Device", android.os.Build.MODEL);
    }

    /**
     * Register to begin sending exception data to bugsnag
     * @param context An android Context
     * @param apiKey Your bugsnag.com project's api key
     */
    public static void register(final Context androidContext, String apiKey) {
        // Require a bugsnag api key
        if(apiKey != null) {
            Bugsnag.apiKey = apiKey;
        } else {
            throw new RuntimeException("The Bugsnag Android Notifier requires a Bugsnag API key.");
        }

        // Require an android context
        if(androidContext == null) {
            throw new RuntimeException("The Bugsnag Android Notifier requires a non-null android Context.");
        }

        // Load or generate a UUID to track unique users
        final SharedPreferences settings = androidContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        userId = settings.getString("userId", null);
        if(userId == null) {
            userId = UUID.randomUUID().toString();

            // Save if for future
            new AsyncTask <Void, Void, Void>() {
                protected Void doInBackground(Void... voi) {
                    SharedPreferences.Editor editor = settings.edit();
                    editor.putString("userId", userId);
                    editor.commit();
                    return null;
                }
            }.execute();
        }

        // Connect our default exception handler
        UncaughtExceptionHandler currentHandler = Thread.getDefaultUncaughtExceptionHandler();
        if(!(currentHandler instanceof BugsnagExceptionHandler)) {
            Thread.setDefaultUncaughtExceptionHandler(new BugsnagExceptionHandler(currentHandler));
        }

        // Load up current package name and version
        try {
            packageName = androidContext.getPackageName();
            PackageInfo pi = androidContext.getPackageManager().getPackageInfo(packageName, 0);
            if(pi.versionName != null) {
                appVersion = pi.versionName;
            }
        } catch (Exception e) {}

        // Prepare the file storage location (this has to be done synchronously)
        filePath = androidContext.getFilesDir().getAbsolutePath() + UNSENT_EXCEPTION_PATH;
        File outFile = new File(filePath);
        outFile.mkdirs();
        diskStorageEnabled = outFile.exists();

        new AsyncTask <Void, Void, Void>() {
            protected Void doInBackground(Void... voi) {
                // Flush any existing exception info
                flushExceptions();
                return null;
            }
        }.execute();

        Log.d(LOG_TAG, "Registered and ready to handle exceptions.");
    }

    /**
     * Fire an exception to bugsnag manually
     * @param e The Throwable object to send
     */
    public static void notify(final Throwable e) {
        notify(e, null);
    }

    /**
     * Fire an exception to bugsnag manually, with some additional data
     * @param e The Throwable object to send
     * @param metaData a Map of String -> String to send with the exception
     */
    public static void notify(final Throwable e, final Map<String,String> metaData) {
        if(apiKey == null) {
            Log.e(LOG_TAG, "You must call register with an apiKey before we can notify of exceptions!");
            return;
        }
        
        if(e != null && diskStorageEnabled && Arrays.asList(notifyReleaseStages).contains(releaseStage)) {
            try {
                // Causes
                JSONArray exceptions = new JSONArray();
                Throwable currentEx = e;
                while(currentEx != null) {
                    JSONObject exception = new JSONObject();
                    exception.put("errorClass", currentEx.getClass().getName());
                    exception.put("message", currentEx.getLocalizedMessage());

                    // Stacktrace
                    JSONArray stacktrace = new JSONArray();
                    StackTraceElement[] stackTrace = currentEx.getStackTrace();
                    for(StackTraceElement el : stackTrace) {
                        try {
                            JSONObject line = new JSONObject();
                            line.put("method", el.getClassName().replace(packageName + ".", "") + "." + el.getMethodName());
                            line.put("file", el.getFileName() == null ? "Unknown" : el.getFileName());
                            line.put("lineNumber", el.getLineNumber());

                            if(el.getClassName().startsWith(packageName)) {
                                line.put("inProject", true);
                            }

                            stacktrace.put(line);
                        } catch(Throwable lineEx) {
                            Log.w(LOG_TAG, lineEx);
                        }
                    }
                    exception.put("stacktrace", stacktrace);

                    currentEx = currentEx.getCause();
                    exceptions.put(exception);
                }
                // Now call the next notify method
                notify(exceptions, metaData);
            } catch (Exception writeEx) {
                Log.w(LOG_TAG, writeEx);
            }
        }
    }
    
    /**
     * Fire an exception to bugsnag manually, with some additional data
     * @param e The Throwable object to send
     * @param metaData a Map of String -> String to send with the exception
     */
    public static void notify(final JSONArray exceptions, final Map<String,String> metaData) {
        if(apiKey == null) {
            Log.e(LOG_TAG, "You must call register with an apiKey before we can notify of exceptions!");
            return;
        }

        // Finalize copies of things that could change
        // NOTE: We are using the mechanics of the AsyncTask closure to make 
        // sure these variables are copied at this point
        final String exceptionUserId = Bugsnag.userId;
        final String exceptionContext = Bugsnag.context;
        final Map<String,Object> exceptionMetaData = Bugsnag.metaData;
        final List<String> exceptionActivityStack = Bugsnag.generateActivityStack();

        // Write and flush the exceptions if we need to
        if(exceptions != null && exceptions.length() > 0 && diskStorageEnabled && Arrays.asList(notifyReleaseStages).contains(releaseStage)) {
            new AsyncTask <Void, Void, Void>() {
                 protected Void doInBackground(Void... voi) {
                     writeExceptionToDisk(exceptions, metaData, exceptionMetaData, exceptionUserId, exceptionContext, exceptionActivityStack);
                     flushExceptions();
                     return null;
                 }
            }.execute();
        }
    }

    /**
     * Sets the current error "context", this is used to help with error grouping
     * @param context A string representing the current state/context of the app
     */
    public static void setContext(String context) {
        Bugsnag.context = context;
    }
    
    public static void setContext(Activity context) {
        Bugsnag.setContext(getActivityName(context));
    }

    /**
     * Sets the current "userId", this is used to identify how many users are
     * affected by an error. If you don't set this yourself, we will
     * automatically create and store a UUID for this device.
     * @param userId A string representing a unique user or device
     */
    public static void setUserId(String userId) {
        Bugsnag.userId = userId;
    }

    /**
     * Sets the current release stage of the app, eg production, staging,
     * or development. Defaults to "production".
     * @param releaseStage A string representing the release stage
     */
    public static void setReleaseStage(String releaseStage) {
        Bugsnag.releaseStage = releaseStage;
    }

    /**
     * Sets the release stages for which we should send exceptions to
     * bugsnag.com. Defaults to ["production"].
     * @param notifyReleaseStages A series of one or more String parameters for each releaseStage
     */
    public static void setNotifyReleaseStages(String... notifyReleaseStages) {
        Bugsnag.notifyReleaseStages = notifyReleaseStages;
    }

    /**
     * Sets whether we should automatically notify bugsnag.com when we detect
     * an exception. Defaults to true.
     * @param automatically A boolean saying if we should automatically notify
     */
    public static void setAutoNotify(boolean autoNotify) {
        Bugsnag.autoNotify = autoNotify;
    }

    public static void addToTab(String tabName, String key, Object value) {
        Object tab = Bugsnag.metaData.get(tabName);
        if(tab == null || !(tab instanceof Map)) {
            tab = new HashMap<String, Object>();
            Bugsnag.metaData.put(tabName, tab);
        }
        if(value != null) {
            ((Map)tab).put(key, value);
        } else {
            ((Map)tab).remove(key);
        }
    }
    
    public static void clearTab(String tabName){
        Bugsnag.metaData.remove(tabName);
    }

    /**
     * Sets whether we should use SSL to communicate with bugsnag.com.
     * Defaults to false.
     * @param useSSL A boolean saying if we should use SSL for communication
     */
     // TODO: Enable this when we implement SSL keystore
     // http://stackoverflow.com/questions/2642777/trusting-all-certificates-using-httpclient-over-https/6378872#6378872
     // public static void setUseSSL(boolean useSSL) {
     //     Bugsnag.useSSL = useSSL;
     // }

    /**
     * Sets the bugsnag endpoint to send exceptions to. 
     * Defaults to notify.bugsnag.com
     * @param endpoint The endpoint to send exceptions to
     */
    public static void setEndpoint(String endpoint) {
        Bugsnag.endpoint = endpoint;
    }
    
    public static void addActivity(Activity activity) {
        Bugsnag.pruneActivityStack();
        Bugsnag.activityStack.add(new WeakReference<Context>(activity));
    }
    
    private static String getActivityName(Object obj) {
        String name = obj.getClass().getName();
        return name.substring(name.lastIndexOf('.') + 1);
    }

    // Package public
    static void pruneActivityStack() {
        List<WeakReference<Context>> toRemove = new LinkedList<WeakReference<Context>>();
        List<String> goodContexts = new LinkedList<String>();
        for(WeakReference<Context> ref : Bugsnag.activityStack){
            if(ref.get() == null){
                toRemove.add(ref);
            }
        }
    }
    
    static List<String> generateActivityStack() {
        Bugsnag.pruneActivityStack();
        
        List<String> goodContexts = new LinkedList<String>();
        for(WeakReference<Context> ref : Bugsnag.activityStack){
            if(ref.get() != null){
                goodContexts.add(getActivityName(ref.get()));
            }
        }
        return goodContexts;
    }
    
    static void setUnityNotifier() {
        NOTIFIER_NAME = "Bugsnag Unity Notifier";
    }

    // Private
    private static String getNotifyUrl() {
        return (useSSL ? "https://" : "http://") + endpoint;
    }
    
    private static JSONArray listToJSONArray(List<Object> source) {
        JSONArray returnValue = new JSONArray();
        for (Object value : source) {
            if(value instanceof Map) {
                returnValue.put(mapToJSONObject((Map<String,Object>)value));
            } else if(value instanceof List) {
                returnValue.put(listToJSONArray((List<Object>)value));
            } else {
                returnValue.put(value);
            }
        }
        return returnValue;
    }
    
    private static JSONObject mapToJSONObject(Map<String,Object> source) {
        JSONObject returnValue = new JSONObject();
        for (Map.Entry<String, Object> entry : source.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            if(value instanceof Map) {
                try{returnValue.put(key, mapToJSONObject((Map<String,Object>)value));}catch(org.json.JSONException ex){}
            } else if(value instanceof List) {
                try{returnValue.put(key, listToJSONArray((List<Object>)value));}catch(org.json.JSONException ex){}
            } else {
                try{returnValue.put(key, value);}catch(org.json.JSONException ex){}
            }
        }
        return returnValue;
    }
    
    private static void writeExceptionToDisk(JSONArray exceptions, Map<String,String> exceptionCustomData, Map<String,Object> exceptionMetaData, String exceptionUserId, String exceptionContext, List<String> exceptionActivityStack) {
        try {
            // Set up the output stream
            int random = new Random().nextInt(99999);
            String filename = filePath + appVersion + "-" + String.valueOf(random) + ".json";
            FileWriter writer = new FileWriter(filename);

            // Outer payload
            JSONObject payload = new JSONObject();
            payload.put("apiKey", apiKey);

            // Notifier info
            JSONObject notifier = new JSONObject();
            notifier.put("name", NOTIFIER_NAME);
            notifier.put("version", NOTIFIER_VERSION);
            notifier.put("url", NOTIFIER_URL);
            payload.put("notifier", notifier);

            // Error
            JSONArray errors = new JSONArray();
            JSONObject error = new JSONObject();

            error.put("userId", exceptionUserId);
            error.put("appVersion", appVersion);
            error.put("osVersion", osVersion);
            error.put("releaseStage", releaseStage);
            error.put("context", exceptionContext);
            error.put("exceptions", exceptions);

            // Create metadata object
            JSONObject sentMetaData = mapToJSONObject(exceptionMetaData);

            JSONObject application = sentMetaData.optJSONObject("application");
            if(application == null) {
                application = new JSONObject();
                sentMetaData.put("application", application);
            }
            
            // App info
            application.put("App Version", appVersion);
            application.put("Package Name", packageName);
            if(exceptionActivityStack != null && exceptionActivityStack.size() > 0) {
                application.put("Top Activity", exceptionActivityStack.get(exceptionActivityStack.size() - 1));
                application.put("activityStack", new JSONArray(exceptionActivityStack));
            }

            JSONObject customData = sentMetaData.optJSONObject("customData");
            if(customData == null) {
                customData = new JSONObject();
                sentMetaData.put("customData", customData);
            }
            
            // Custom data
            if(exceptionCustomData != null) {
                mergeJSONObjects(customData, new JSONObject(exceptionCustomData));
            }

            error.put("metaData", sentMetaData);

            // Add the error to the errors list
            errors.put(error);
            payload.put("events", errors);

            // Write the errors out to the file
            writer.write(payload.toString());
            writer.flush();
            writer.close();
            Log.d(LOG_TAG, "Writing new " + ((JSONObject)exceptions.get(0)).getString("errorClass") + " exception to disk.");
        } catch (Exception writeEx) {
            Log.w(LOG_TAG, writeEx);
        }
    }

    private static void sendExceptionData(File file) {
        try {
            String urlString = getNotifyUrl();
            boolean sent = false;
            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            try {
                // Set up the connection
                conn.setDoOutput(true);
                conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");

                // Read from the file and send it
                FileInputStream is = new FileInputStream(file);
                OutputStream os = conn.getOutputStream();
                byte[] buffer = new byte[4096];
                int numRead;
                while((numRead = is.read(buffer)) >= 0) {
                  os.write(buffer, 0, numRead);
                }
                os.flush();
                os.close();
                is.close();

                // Flush the request through
                int response = conn.getResponseCode();
                sent = true;
                Log.d(LOG_TAG, String.format("Sent exception file %s to %s. Got response code %d", file.getName(), urlString, response));
            } catch(Throwable ei) {
                // Ignore any file stream issues
                Log.w(LOG_TAG, ei);
            } finally {
                // Delete file now we've sent the exceptions
                if(sent) {
                  file.delete();
                }
                conn.disconnect();
            }
        } catch(IOException e) {
            // Ignore any connection failure when trying to open the connection
            // We can try again next time
            // TODO: Warn users they need to add the following permission to manifest:
            // <uses-permission android:name="android.permission.INTERNET" />
            Log.w(LOG_TAG, e);
        }
    }

    private static synchronized void flushExceptions() {
        File exceptionDir = new File(filePath);
        if(exceptionDir.exists() && exceptionDir.isDirectory()) {
            File[] exceptions = exceptionDir.listFiles();
            for(File f : exceptions) {
                if(f.exists() && f.isFile()) {
                    sendExceptionData(f);
                }
            }
        }
    }
    
    private static void mergeJSONObjects(JSONObject dest, JSONObject source) {
        try {
            if(source != null) {
                Iterator<?> keys = source.keys();
                
                while( keys.hasNext() ){
                    String key = (String)keys.next();
                    dest.put(key, source.get(key));
                }
            }
        } catch(Exception e) {
            Log.w(LOG_TAG, e);
        }
    }

    // Wrapper class to send uncaught exceptions to bugsnag
    private static class BugsnagExceptionHandler implements UncaughtExceptionHandler {
        private UncaughtExceptionHandler defaultExceptionHandler;

        public BugsnagExceptionHandler(UncaughtExceptionHandler defaultExceptionHandlerIn) {
            defaultExceptionHandler = defaultExceptionHandlerIn;
        }

        public void uncaughtException(Thread t, Throwable e) {
            if(autoNotify) {
                Bugsnag.notify(e);
            }

            defaultExceptionHandler.uncaughtException(t, e);
        }
    }
}
