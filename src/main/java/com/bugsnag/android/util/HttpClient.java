package com.bugsnag.android;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import java.net.HttpURLConnection;
import java.net.URL;

import android.os.AsyncTask;

public class HttpClient {
    public interface ResponseHandler {
        public void onSuccess();
        public void onFailure(Throwable e);
    }

    public static void post(final String urlString, final InputStream payload, final ResponseHandler handler) {
        new AsyncTask <Void, Void, Throwable>() {
            @Override
            protected Throwable doInBackground(Void... voi) {
                return postSync(urlString, payload);
            }

            @Override
            protected void onPostExecute(Throwable e) {
                if(e == null) {
                    handler.onSuccess();
                } else {
                    handler.onFailure(e);
                }
            }
        }.execute();
    }

    private static Throwable postSync(String urlString, InputStream payload) {
        Throwable exception = null;
        HttpURLConnection conn = null;
        try {
            URL url = new URL(urlString);
            conn = (HttpURLConnection) url.openConnection();
            conn.setDoOutput(true);
            conn.setChunkedStreamingMode(0);
            conn.addRequestProperty("Content-Type", "application/json");

            OutputStream out = null;
            try {
                out = conn.getOutputStream();

                // Send request headers and body
                byte[] buffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = payload.read(buffer)) != -1)
                {
                    out.write(buffer, 0, bytesRead);
                }
            } finally {
                if(out != null) {
                    out.close();
                }
            }

            // End the request, get the response code
            int status = conn.getResponseCode();
            if(status / 100 != 2) {
                exception = new IOException(String.format("Got non-200 response code (%d) from %s", status, urlString));
            }
        } catch (IOException e) {
            exception = new IOException(String.format("Network error when posting to %s", urlString), e);
        } finally {
            if(conn != null) {
                conn.disconnect();
            }
        }

        return exception;
    }
}
