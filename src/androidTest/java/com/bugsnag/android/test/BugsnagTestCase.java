package com.bugsnag.android;

import java.io.StringWriter;

import org.json.JSONException;
import org.json.JSONObject;

import android.test.AndroidTestCase;

public class BugsnagTestCase extends AndroidTestCase {
    protected String streamableToString(JsonStream.Streamable streamable) {
        StringWriter writer = new StringWriter();
        JsonStream jsonStream = new JsonStream(writer);

        streamable.toStream(jsonStream);

        return writer.toString();
    }

    protected String streamableToString(HttpClient.Streamable streamable) {
        StringWriter writer = new StringWriter();
        streamable.toStream(writer);
        return writer.toString();
    }

    protected JSONObject streamableToJson(JsonStream.Streamable streamable) {
        JSONObject json = null;

        try {
            return new JSONObject(streamableToString(streamable));
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return json;
    }

    protected JSONObject streamableToJson(HttpClient.Streamable streamable) {
        JSONObject json = null;

        try {
            return new JSONObject(streamableToString(streamable));
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return json;
    }
}
