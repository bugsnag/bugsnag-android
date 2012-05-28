/*
    Bugsnag Notifier for Android
    Copyright (c) 2011 Bugsnag
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
import java.lang.Thread.UncaughtExceptionHandler;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import org.json.JSONObject;
import org.json.JSONArray;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.PackageInfo;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.text.TextUtils;
import android.util.Log;
import android.util.Xml;


public class Bugsnag {
    private static final String PREFS_NAME = "Bugsnag";
    private static final String LOG_TAG = "Bugsnag";

    // Basic settings
    private static final String BUGSNAG_ENDPOINT = "http://api.bugsnag.com/notify";
    private static String bugsnagEndpoint = BUGSNAG_ENDPOINT;

    private static final String NOTIFIER_NAME = "Android Bugsnag Notifier";
    private static final String NOTIFIER_VERSION = "1.0.0";
    private static final String NOTIFIER_URL = "http://www.bugsnag.com";

    private static final String UNSENT_EXCEPTION_PATH = "/unsent_bugsnag_exceptions/";

    // Exception meta-data
    private static String environmentName = "production";
    private static String packageName = "unknown";
    private static String versionName = "unknown";
    private static String phoneModel = android.os.Build.MODEL;
    private static String androidVersion = android.os.Build.VERSION.RELEASE;

    // Anything extra the app wants to add
    private static Map<String, String> extraData;

    // Bugsnag api key
    private static String apiKey;

    // Exception storage info
    private static boolean notifyOnlyProduction = false;
    private static String filePath;
    private static boolean diskStorageEnabled = false;
    private static String userId;

    // Wrapper class to send uncaught exceptions to bugsnag
    private static class BugsnagExceptionHandler implements UncaughtExceptionHandler {
        private UncaughtExceptionHandler defaultExceptionHandler;

        public BugsnagExceptionHandler(UncaughtExceptionHandler defaultExceptionHandlerIn) {
            defaultExceptionHandler = defaultExceptionHandlerIn;
        }

        public void uncaughtException(Thread t, Throwable e) {
            Bugsnag.notify(e);
            defaultExceptionHandler.uncaughtException(t, e);
        }
    }

    // Register to send exceptions to bugsnag
    public static void register(Context context, String apiKey) {
        register(context, apiKey, "production", true);
    }

    public static void register(Context context, String apiKey, String environmentName) {
        register(context, apiKey, environmentName, true);
    }

    public static void register(Context context, String apiKey, String environmentName, boolean notifyOnlyProduction) {
        // Require a bugsnag api key
        if(apiKey != null) {
            Bugsnag.apiKey = apiKey;
        } else {
            throw new RuntimeException("The Bugsnag Android Notifier requires a Bugsnag API key.");
        }

        // Fill in environment name if passed
        if(environmentName != null) {
            Bugsnag.environmentName = environmentName;
        }

        // Check which exception types to notify
        Bugsnag.notifyOnlyProduction = notifyOnlyProduction;

        // Load or generate a UUID to track unique users
        final SharedPreferences settings = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
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
        if(!(currentHandler instanceof BugsnagExceptionHandler) && (environmentName.equals("production") || !notifyOnlyProduction)) {
            Thread.setDefaultUncaughtExceptionHandler(new BugsnagExceptionHandler(currentHandler));
        }

        // Load up current package name and version
        try {
            packageName = context.getPackageName();
            PackageInfo pi = context.getPackageManager().getPackageInfo(packageName, 0);
            if(pi.versionName != null) {
                versionName = pi.versionName;
            }
        } catch (Exception e) {}

        // Prepare the file storage location
        // TODO: Does this need to be done in a background thread?
        filePath = context.getFilesDir().getAbsolutePath() + UNSENT_EXCEPTION_PATH;
        File outFile = new File(filePath);
        outFile.mkdirs();
        diskStorageEnabled = outFile.exists();

        Log.d(LOG_TAG, "Registered and ready to handle exceptions.");

        // Flush any existing exception info
        new AsyncTask <Void, Void, Void>() {
            protected Void doInBackground(Void... voi) {
                flushExceptions();
                return null;
            }
        }.execute();
    }

    /**
     * Add a custom set of key/value data that will be sent as session data with each notification
     * @param extraData a Map of String -> String
     */
    public static void setExtraData(Map<String,String> extraData) {
        Bugsnag.extraData = extraData;
    }

    // Fire an exception to bugsnag manually
    public static void notify(final Throwable e) {
        notify(e, null);
    }

    public static void notify(final Throwable e, final Map<String,String> metaData) {
        if(e != null && diskStorageEnabled) {
            new AsyncTask <Void, Void, Void>() {
                 protected Void doInBackground(Void... voi) {
                     writeExceptionToDisk(e, metaData);
                     flushExceptions();
                     return null;
                 }
            }.execute();
        }
    }

    public static void setEndpoint(String endpoint) {
        bugsnagEndpoint = endpoint;
    }

    private static void writeExceptionToDisk(Throwable e, final Map<String,String> customData) {
        try {
            // Set up the output stream
            int random = new Random().nextInt(99999);
            String filename = filePath + versionName + "-" + String.valueOf(random) + ".json";
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

            error.put("userId", userId);
            error.put("appVersion", versionName);
            error.put("releaseStage", environmentName);
            error.put("context", ""); // TODO

            // Causes
            JSONArray exceptions = new JSONArray();
            Throwable currentEx = e;
            while(currentEx != null) {
                JSONObject exception = new JSONObject();
                exception.put("errorClass", e.getClass().getName());
                exception.put("message", e.getLocalizedMessage());

                // Stacktrace
                JSONArray stacktrace = new JSONArray();
                StackTraceElement[] stackTrace = currentEx.getStackTrace();
                for(StackTraceElement el : stackTrace) {
                    try {
                        JSONObject line = new JSONObject();
                        line.put("method", el.getClassName().replace(packageName, "") + "." + el.getMethodName());
                        line.put("file", el.getFileName() == null ? "Unknown" : el.getFileName());
                        line.put("lineNumber", el.getLineNumber());

                        if(el.getClassName().startsWith(packageName)) {
                            line.put("inProject", true);
                        }

                        stacktrace.put(line);
                    } catch(Throwable lineEx) {
                        lineEx.printStackTrace();
                    }
                }
                exception.put("stacktrace", stacktrace);

                currentEx = currentEx.getCause();
                exceptions.put(exception);
            }
            error.put("exceptions", exceptions);

            // Create metadata object
            JSONObject metaData = new JSONObject();

            // Device info
            JSONObject device = new JSONObject();
            device.put("osVersion", androidVersion);
            device.put("device", phoneModel);
            metaData.put("device", device);

            // Custom data
            JSONObject customDataObj = new JSONObject();
            if(extraData != null && !extraData.isEmpty()) {
                for(Map.Entry<String,String> extra : extraData.entrySet()) {
                    customDataObj.put(extra.getKey(), extra.getValue());
                }
            }
            if(customData != null && !customData.isEmpty()) {
                for(Map.Entry<String,String> extra : customData.entrySet()) {
                    customDataObj.put(extra.getKey(), extra.getValue());
                }
            }
            metaData.put("customData", customDataObj);

            error.put("metaData", metaData);

            // Add the error to the errors list
            errors.put(error);
            payload.put("errors", errors);

            // Write the errors out to the file
            writer.write(payload.toString());
            writer.flush();
            writer.close();
            Log.d(LOG_TAG, "Writing new " + e.getClass().getName() + " exception to disk.");
        } catch (Exception writeEx) {
            writeEx.printStackTrace();
        }
    }

    private static void sendExceptionData(File file) {
        try {
            URL url = new URL(bugsnagEndpoint);
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
                Log.d(LOG_TAG, "Sent exception file " + file.getName() + " to bugsnag. Got response code " + String.valueOf(response));
            } catch(Throwable ei) {
                // Ignore any file stream issues
                ei.printStackTrace();
            } finally {
                // Delete file now we've sent the exceptions
                file.delete();
                conn.disconnect();
            }
        } catch(IOException e) {
            // Ignore any connection failure when trying to open the connection
            // We can try again next time
            // TODO: Warn users they need to add the following permission to manifest:
            // <uses-permission android:name="android.permission.INTERNET" /> 
            e.printStackTrace();
        }
    }

    private static void flushExceptions() {
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
}
