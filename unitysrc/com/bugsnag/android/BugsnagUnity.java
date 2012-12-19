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
    
import com.unity3d.player.UnityPlayer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.json.JSONObject;
import org.json.JSONArray;

public class BugsnagUnity {
    public static void setUserId(String userId) {
        Bugsnag.setUserId(userId);
    }
    
    public static void setContext(String context) {
        Bugsnag.setContext(context);
    }
    
    public static void setReleaseStage(String releaseStage) {
        Bugsnag.setReleaseStage(releaseStage);
    }
    
    public static void setUseSSL(boolean useSSL) {
        //TODO Get this working in android then uncomment
        //Bugsnag.setUseSSL(useSSL);
    }
    
    public static void setAutoNotify(boolean autoNotify) {
        Bugsnag.setAutoNotify(autoNotify);
    }
    
    public static void notify(final String errorClass, final String errorMessage, final String stackTrace) {
        JSONArray exceptions = new JSONArray();
        JSONObject exception = new JSONObject();
        exceptions.put(exception);
        
        try {exception.put("errorClass", errorClass);} catch(org.json.JSONException ex){}
        try {exception.put("message", errorMessage);} catch(org.json.JSONException ex){}

        // Stacktrace
        Pattern manualUnityNotifyPattern = Pattern.compile("at\\s(\\S+).+?(<filename unknown>|\\S+):(\\d*)\\s*");
        Pattern autoUnityNotifyPattern = Pattern.compile("\\s*(\\S+) \\(.*?(?:at (\\S*?):(\\d*)|\\n)");
        
        JSONArray stackTraceArray;
        
        if(stackTrace.startsWith("  at ")) {
            stackTraceArray = parseStackTrace(manualUnityNotifyPattern, stackTrace);
        } else {
            stackTraceArray = parseStackTrace(autoUnityNotifyPattern, stackTrace);
        }
                
        try {exception.put("stacktrace", stackTraceArray);} catch(org.json.JSONException ex){}
                
        Bugsnag.notify(exceptions, null);
    }
    
    private static JSONArray parseStackTrace(Pattern pattern, String stackTrace) {
        JSONArray stackTraceArray = new JSONArray();
        Matcher match = pattern.matcher(stackTrace);
        while(match.find()) {
            JSONObject line = new JSONObject();
            int groupCount = match.groupCount();
            try {
                String value = groupCount >= 1 && match.group(1) != null ? match.group(1) : "unknown method";
                line.put("method", value.trim());
            } catch(org.json.JSONException ex){}
            
            try {
                String value = groupCount >= 2 && match.group(2) != null ? match.group(2) : "unknown file";
                line.put("file", value.trim());
            } catch(org.json.JSONException ex){}
            
            try {
                int value = 0;
                if(groupCount >= 3 && match.group(3) != null) {
                    try {
                        value = Integer.parseInt(match.group(3));
                    } catch(NumberFormatException ex) {}
                }
                
                line.put("lineNumber", value);
            } catch(org.json.JSONException ex){}
            
            stackTraceArray.put(line);
        }
        return stackTraceArray;
    }
    
    public static void register(final String apiKey) {
        UnityPlayer.currentActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Bugsnag.register(UnityPlayer.currentActivity, apiKey);
                Bugsnag.setUnityNotifier();
            }
        });
    }
    
    public static void addToTab(String tabName, String attributeName, String attributeValue) {
        Bugsnag.addToTab(tabName, attributeName, attributeValue);
    }
    
    public static void clearTab(String tabName) {
        Bugsnag.clearTab(tabName);
    }
}