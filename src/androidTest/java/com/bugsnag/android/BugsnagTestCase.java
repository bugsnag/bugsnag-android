package com.bugsnag.android;

import android.content.Context;
import android.content.SharedPreferences;
import android.test.AndroidTestCase;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.StringWriter;

@Deprecated
public class BugsnagTestCase extends AndroidTestCase {
    protected String streamableToString(JsonStream.Streamable streamable) throws IOException {
        StringWriter writer = new StringWriter();
        JsonStream jsonStream = new JsonStream(writer);
        streamable.toStream(jsonStream);

        return writer.toString();
    }

    protected JSONObject streamableToJson(JsonStream.Streamable streamable) throws JSONException, IOException {
        return new JSONObject(streamableToString(streamable));
    }

    protected JSONArray streamableToJsonArray(JsonStream.Streamable streamable) throws JSONException, IOException  {
        return new JSONArray(streamableToString(streamable));
    }

    protected SharedPreferences getSharedPrefs() {
        return getContext().getSharedPreferences("com.bugsnag.android", Context.MODE_PRIVATE);
    }
}
